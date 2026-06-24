package net.diveon.backend.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class UserTierService {

    private final UserRepository userRepository;
    private final SolveResultRepository solveResultRepository;

    public UserTierService(UserRepository userRepository, SolveResultRepository solveResultRepository) {
        this.userRepository = userRepository;
        this.solveResultRepository = solveResultRepository;
    }

    @Transactional
    public void refreshTierScore(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        Integer score = solveResultRepository.calculateTierScoreByUserId(userId);
        int tierScore = score == null ? 0 : score;
        user.updateTierScore(tierScore, calculateTier(tierScore));
    }

    private int calculateTier(int score) {
        if (score >= 6600) return 7;
        if (score >= 5400) return 6;
        if (score >= 4000) return 5;
        if (score >= 2400) return 4;
        if (score >= 1350) return 3;
        if (score >= 600) return 2;
        if (score >= 150) return 1;
        return 0;
    }
}
