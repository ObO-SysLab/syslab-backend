package net.diveon.backend.domain.notification.service;

// 댓글이 달렸을 때 받는 사람에게 알림을 남긴다. 댓글 서비스는 이 인터페이스만 안다.
public interface CommentNotifier {

    void problemComment(long recipientId, long actorId, String actorNickname,
                        long commentId, long probId, String problemTitle, boolean reply);

    void groupPostComment(long recipientId, long actorId, String actorNickname,
                          long commentId, long groupId, String postTitle);

    CommentNotifier NONE = new CommentNotifier() {
        @Override
        public void problemComment(long recipientId, long actorId, String actorNickname,
                                   long commentId, long probId, String problemTitle, boolean reply) {
        }

        @Override
        public void groupPostComment(long recipientId, long actorId, String actorNickname,
                                     long commentId, long groupId, String postTitle) {
        }
    };
}
