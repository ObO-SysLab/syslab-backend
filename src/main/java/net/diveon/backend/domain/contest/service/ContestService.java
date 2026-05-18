package net.diveon.backend.domain.contest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.ContestCreateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestCreateResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestTag;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestTagRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ContestTagRepository contestTagRepository;
    private final UserRepository userRepository;

    public ContestService(ContestRepository contestRepository,
                          ContestParticipantRepository contestParticipantRepository,
                          ContestTagRepository contestTagRepository,
                          UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.contestTagRepository = contestTagRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ContestCreateResponse createContest(Long userId, ContestCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Contest contest = new Contest(
                user, null,
                request.getTitle(), request.getDescription(),
                request.getContestType(), request.getParticipationType(), request.getVisibility(),
                request.getStartTime(), request.getEndTime(),
                request.getRules(), null, null
        );
        contestRepository.save(contest);

        contestParticipantRepository.save(new ContestParticipant(contest, user, ContestParticipant.ContestRole.ADMIN));

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<ContestTag> tags = request.getTags().stream()
                    .map(tag -> new ContestTag(contest, tag))
                    .toList();
            contestTagRepository.saveAll(tags);
        }

        return new ContestCreateResponse(contest.getId(), contest.getTitle());
    }
}
