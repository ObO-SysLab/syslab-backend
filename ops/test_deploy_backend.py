"""Exercise release failure boundaries with fake commands; never connects to AWS or a host."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
REVISION = "a" * 40
IMAGE = "290373175858.dkr.ecr.ap-northeast-2.amazonaws.com/syslab-backend@sha256:" + "b" * 64
FAKES = r'''#!/usr/bin/env python3
import json,os,pathlib,sys
name=pathlib.Path(sys.argv[0]).name
a=sys.argv[1:]
root=pathlib.Path(os.environ['FAKE_ROOT'])
with (root/'commands.jsonl').open('a') as f:f.write(json.dumps([name]+a)+'\n')
state=json.loads((root/'state.json').read_text())
def save(): (root/'state.json').write_text(json.dumps(state))
if name=='id': print(0)
elif name=='stat':
 print(0 if a[1]=='%u' else oct(pathlib.Path(a[2]).stat().st_mode & 0o777)[2:])
elif name=='sleep': pass
elif name=='aws':
 if a[:2]==['ecr','get-login-password']:print('synthetic-ecr-password')
 elif a[:2]==['ec2','describe-instances']:
  print(json.dumps({'Reservations':[{'Instances':[{'State':{'Name':'running'},'PublicIpAddress':'3.34.216.202','PublicDnsName':'api.example','SecurityGroups':[{'GroupId':'sg-1234'}]}]}]}))
 elif a[:2]==['ec2','describe-security-groups']:
  print(json.dumps({'SecurityGroups':[{'GroupId':'sg-1234','Tags':[{'Key':'DeploymentIngress','Value':'diveon-api'}]}]}))
 elif a[:2]==['ec2','authorize-security-group-ingress']:
  state['sshRule']='sgr-0123';save();print('sgr-0123')
 elif a[:2]==['ec2','revoke-security-group-ingress']:
  assert a[-1]=='sgr-0123';state['sshRule']=None;save();print('{}')
 else:sys.exit('Unhandled fake AWS command')
elif name=='ssh':
 assert 'StrictHostKeyChecking=yes' in a
 assert any(x.startswith('UserKnownHostsFile=') for x in a)
 sys.stdin.read()
 sys.exit(int(os.environ.get('FAKE_SSH_EXIT','0')))
elif name=='curl':
 print('8.8.8.8' if a[-1]=='https://checkip.amazonaws.com' else ('403' if a[-1].endswith('/api/profile/show') else os.environ.get('FAKE_HEALTH','200')))
elif name=='docker':
 if a[0]=='login':sys.stdin.read()
 elif a[0]=='pull':print(a[1])
 elif a[:2]==['image','inspect']:print(a[-1])
 elif a[:3]==['exec','redis','redis-cli']:print('PONG')
 elif a[:2]==['container','inspect']:
  if a[-1] not in state:sys.exit(1)
  print(json.dumps([state[a[-1]]]))
 elif a[0]=='inspect':
  if a[-1] not in state:sys.exit(1)
  c=state[a[-1]];fmt=a[a.index('--format')+1]
  if fmt=='{{.State.Running}}':print(str(c['running']).lower())
  elif fmt=='{{.Config.Image}}':print(c['image'])
  elif 'net.diveon.source' in fmt:print(c.get('revision',''))
  else:sys.exit('Unhandled fake inspect format')
 elif a[0]=='stop':state[a[-1]]['running']=False;save()
 elif a[0]=='update':state[a[-1]]['restart']='no';save()
 elif a[0]=='rename':state[a[2]]=state.pop(a[1]);save()
 elif a[0]=='run':
  name=a[a.index('--name')+1];assert name not in state
  state[name]={'image':a[-1],'revision':a[a.index('--label')+1].split('=',1)[1],'running':True,'restart':'always'};save()
  if os.environ.get('FAKE_RUN_FAIL'):sys.exit(1)
  print('new-container-id')
 elif a[0]=='logs':print('synthetic diagnostic')
 else:sys.exit('Unhandled fake Docker command')
else:sys.exit('Unhandled fake executable')
'''


@unittest.skipUnless(os.name == 'posix' and shutil.which('bash'), 'Linux shell contract')
class DeploymentBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='diveon-deploy-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        for command in ['id', 'stat', 'sleep', 'aws', 'curl', 'docker', 'ssh']:
            file = self.bin / command
            file.write_text(FAKES)
            file.chmod(0o700)
        self.initial = {'syslab-backend': {'image': 'previous-image', 'running': True, 'restart': 'always'}}
        (self.root / 'state.json').write_text(json.dumps(self.initial))
        (self.root / 'commands.jsonl').touch()
        for directory in ['etc/diveon', 'home/ec2-user', 'run', 'var/lock']:
            (self.root / directory).mkdir(parents=True)
        self.config = self.root / 'etc/diveon/application-secret.yml'
        self.config.write_text('synthetic: true\n')
        self.config.chmod(0o600)
        (self.root / 'home/ec2-user/syslab-key.pem').write_text('synthetic-test-key')
        script = (HERE / 'deploy-backend.sh').read_text()
        for prefix in ['/var/lock/', '/etc/diveon/', '/home/ec2-user/', '/var/lib/diveon/', '/run/diveon-docker-login.']:
            script = script.replace(prefix, str(self.root) + prefix)
        self.script = self.root / 'deploy-backend.sh'
        self.script.write_text(script)
        self.env = dict(os.environ, PATH=str(self.bin) + os.pathsep + os.environ['PATH'], FAKE_ROOT=str(self.root))

    def state(self):
        return json.loads((self.root / 'state.json').read_text())

    def commands(self):
        return [json.loads(line) for line in (self.root / 'commands.jsonl').read_text().splitlines()]

    def run_deploy(self, **env):
        return subprocess.run(['bash', str(self.script), IMAGE, REVISION], env=dict(self.env, **env), text=True, capture_output=True, timeout=30)

    def assert_preserved(self):
        old = [c for name, c in self.state().items() if name.startswith('syslab-backend-prev-')]
        self.assertEqual(len(old), 1)
        self.assertEqual(old[0]['image'], 'previous-image')
        self.assertFalse(old[0]['running'])
        self.assertEqual(old[0]['restart'], 'no')
        self.assertFalse(any(c[0] == 'docker' and c[1] in ['rm', 'start', 'prune'] for c in self.commands()))

    def test_success_preserves_old_and_runs_digest_with_external_config(self):
        result = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_preserved()
        self.assertEqual(self.state()['syslab-backend']['image'], IMAGE)
        run = next(c for c in self.commands() if c[:2] == ['docker', 'run'])
        self.assertIn('SPRING_JPA_HIBERNATE_DDL_AUTO=validate', run)
        self.assertIn('SPRING_PROFILES_ACTIVE=prod', run)
        self.assertTrue(any('dst=/run/diveon/application-secret.yml,readonly' in v for v in run))

    def test_unhealthy_candidate_is_stopped_without_restarting_old(self):
        result = self.run_deploy(FAKE_HEALTH='503')
        self.assertNotEqual(result.returncode, 0)
        self.assert_preserved()
        failed = [c for name, c in self.state().items() if name.startswith('syslab-backend-failed-')]
        self.assertEqual(len(failed), 1)
        self.assertFalse(failed[0]['running'])
        self.assertEqual(failed[0]['restart'], 'no')

    def test_partially_created_candidate_is_stopped_on_run_error(self):
        self.assertNotEqual(self.run_deploy(FAKE_RUN_FAIL='1').returncode, 0)
        self.assert_preserved()
        self.assertFalse(any(c['running'] for c in self.state().values()))

    def test_missing_secret_refuses_before_stopping_old(self):
        self.config.unlink()
        self.assertNotEqual(self.run_deploy().returncode, 0)
        self.assertEqual(self.state(), self.initial)

    def test_readable_secret_refuses_before_stopping_old(self):
        self.config.chmod(0o644)
        self.assertNotEqual(self.run_deploy().returncode, 0)
        self.assertEqual(self.state(), self.initial)

    def test_remote_failure_revokes_only_the_owned_ssh_rule(self):
        key_temp = self.root / 'runner'
        key_temp.mkdir()
        env = dict(self.env, RUNNER_TEMP=str(key_temp), GITHUB_RUN_ID='1234', GITHUB_RUN_ATTEMPT='1',
                   EC2_HOST='3.34.216.202', EC2_USER='ec2-user', EC2_KEY='synthetic-key', IMAGE_URI=IMAGE,
                   SOURCE_REVISION=REVISION, AWS_REGION='ap-northeast-2', FAKE_SSH_EXIT='42')
        result = subprocess.run(['bash', str(HERE / 'github-deploy.sh')], env=env, text=True, capture_output=True, timeout=30)
        self.assertEqual(result.returncode, 42, result.stderr)
        self.assertIsNone(self.state()['sshRule'])
        self.assertEqual(list(key_temp.iterdir()), [])
        revoke = [c for c in self.commands() if c[:3] == ['aws', 'ec2', 'revoke-security-group-ingress']]
        self.assertEqual(len(revoke), 1)
        self.assertEqual(revoke[0][-1], 'sgr-0123')


if __name__ == '__main__':
    unittest.main()
