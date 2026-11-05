#!/usr/bin/env bash
# GitHub-hosted runner only. No credentials or private configuration enter the artifact.
set -euo pipefail
umask 077
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
: "${RUNNER_TEMP:?}" "${GITHUB_RUN_ID:?}" "${GITHUB_RUN_ATTEMPT:?}"
: "${EC2_HOST:?}" "${EC2_USER:?}" "${EC2_KEY:?}" "${IMAGE_URI:?}" "${SOURCE_REVISION:?}"
[[ "$EC2_USER" =~ ^[a-z_][a-z0-9_-]*$ ]]
[[ "$SOURCE_REVISION" =~ ^[0-9a-f]{40}$ ]]
[[ "$IMAGE_URI" =~ ^290373175858\.dkr\.ecr\.ap-northeast-2\.amazonaws\.com/[a-z0-9][a-z0-9._/-]*@sha256:[0-9a-f]{64}$ ]]
test "$AWS_REGION" = ap-northeast-2
work=$(mktemp -d "$RUNNER_TEMP/diveon-deploy.XXXXXX")
group_id=''
rule_id=''
authorization_attempted=false
description="diveon-github-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
cleanup() {
  status=$?
  trap - EXIT
  set +e
  if "$authorization_attempted"; then
    # Recover our rule ID if an accepted request lost its response. Never revoke someone else's rule.
    if [[ -z "$rule_id" || "$rule_id" == None ]]; then
      aws ec2 describe-security-group-rules --filters "Name=group-id,Values=$group_id" --output json > "$work/rules.json"
      if [[ $? == 0 ]]; then
        rule_id=$(python3 -c 'import json,sys; rules=[r for r in json.load(open(sys.argv[1]))["SecurityGroupRules"] if r.get("Description")==sys.argv[2] and r.get("CidrIpv4")==sys.argv[3] and r.get("FromPort")==22 and r.get("ToPort")==22 and r.get("IsEgress") is False]; assert len(rules)<=1,"Ambiguous owned SSH rules"; print(rules[0]["SecurityGroupRuleId"] if rules else "")' "$work/rules.json" "$description" "$runner_cidr")
        if [[ $? != 0 ]]; then status=1; fi
      else
        echo 'Unable to verify temporary SSH cleanup; inspect the deployment run rule.' >&2
        status=1
      fi
    fi
    if [[ "$rule_id" =~ ^sgr-[0-9a-f]+$ ]]; then
      if ! aws ec2 revoke-security-group-ingress --group-id "$group_id" --security-group-rule-ids "$rule_id" > /dev/null; then
        echo "Temporary SSH rule cleanup failed: $rule_id in $group_id" >&2
        status=1
      fi
    fi
  fi
  # work is a fresh mktemp child of RUNNER_TEMP, never a supplied or computed deployment directory.
  rm -f -- "$work/key" "$work/instances.json" "$work/groups.json" "$work/permission.json" "$work/rules.json"
  rmdir -- "$work"
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' HUP INT TERM
printf '%s\n' "$EC2_KEY" > "$work/key"
unset EC2_KEY
aws ec2 describe-instances --instance-ids i-0c2732ec95cc07477 --output json > "$work/instances.json"
test "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["Reservations"][0]["Instances"][0]["State"]["Name"])' "$work/instances.json")" = running
public_ip=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["Reservations"][0]["Instances"][0]["PublicIpAddress"])' "$work/instances.json")
public_dns=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["Reservations"][0]["Instances"][0].get("PublicDnsName",""))' "$work/instances.json")
test "$public_ip" = 3.34.216.202
[[ "$EC2_HOST" == "$public_ip" || "$EC2_HOST" == "$public_dns" ]]
mapfile -t attached_groups < <(python3 -c 'import json,sys; print("\n".join(g["GroupId"] for g in json.load(open(sys.argv[1]))["Reservations"][0]["Instances"][0]["SecurityGroups"]))' "$work/instances.json")
aws ec2 describe-security-groups --group-ids "${attached_groups[@]}" --output json > "$work/groups.json"
group_id=$(python3 -c 'import json,sys; groups=[g for g in json.load(open(sys.argv[1]))["SecurityGroups"] if any(t["Key"]=="DeploymentIngress" and t["Value"]=="diveon-api" for t in g.get("Tags",[]))]; assert len(groups)==1,"Exactly one attached DeploymentIngress=diveon-api group is required"; print(groups[0]["GroupId"])' "$work/groups.json")
runner_ip=$(curl --fail --silent --show-error --max-time 10 --retry 2 https://checkip.amazonaws.com)
runner_cidr=$(python3 -c 'import ipaddress,sys; a=ipaddress.ip_address(sys.argv[1].strip()); assert a.version == 4 and a.is_global; print(str(a)+"/32")' "$runner_ip")
python3 -c 'import json,sys; print(json.dumps([{"IpProtocol":"tcp","FromPort":22,"ToPort":22,"IpRanges":[{"CidrIp":sys.argv[1],"Description":sys.argv[2]}]}]))' "$runner_cidr" "$description" > "$work/permission.json"
authorization_attempted=true
rule_id=$(aws ec2 authorize-security-group-ingress --group-id "$group_id" --ip-permissions "file://$work/permission.json" --query 'SecurityGroupRules[0].SecurityGroupRuleId' --output text)
[[ "$rule_id" =~ ^sgr-[0-9a-f]+$ ]]
ssh -F /dev/null -i "$work/key" -o BatchMode=yes -o IdentitiesOnly=yes -o IdentityAgent=none \
  -o ForwardAgent=no -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$SCRIPT_DIR/api_known_hosts" \
  -o "HostKeyAlias=$public_ip" -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=3 \
  "$EC2_USER@$EC2_HOST" "sudo -n bash -s -- '$IMAGE_URI' '$SOURCE_REVISION'" < "$SCRIPT_DIR/deploy-backend.sh"
