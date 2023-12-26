package com.drrr.domain.log.service;

import static com.drrr.domain.log.entity.post.QMemberPostLog.memberPostLog;

import com.drrr.core.exception.log.LoggingExceptionCode;
import com.drrr.domain.log.entity.history.MemberPostHistory;
import com.drrr.domain.log.entity.post.MemberPostLog;
import com.drrr.domain.log.repository.MemberPostHistoryRepository;
import com.drrr.domain.log.repository.MemberPostLogRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class LogUpdateService {
    private final JPAQueryFactory queryFactory;
    private final MemberPostHistoryRepository memberPostHistoryRepository;
    private final MemberPostLogRepository memberPostLogRepository;

    public void insertMemberPostReadLog(final Long memberId, final Long postId) {
        memberPostLogRepository.findByPostIdAndMemberId(memberId, postId)
                .orElseGet(() -> memberPostLogRepository.save(MemberPostLog.builder()
                        .memberId(memberId)
                        .postId(postId)
                        .isRead(true)
                        .isRecommended(false)
                        .build()));
    }

    public void updateMemberPostRecommendLog(final Long memberId, final List<Long> postIds) {
        //해당 유저의 추천받은 기술블로그 ids와 member id로 log 정보 가져오기
        final List<MemberPostLog> logs = queryFactory
                .selectFrom(memberPostLog)
                .where(postIdsInOrEq(postIds), memberPostLog.memberId.eq(memberId))
                .fetch();

        //추천 받은적이 없고 읽었던 기술블로그가 아니여야함
        //getFilteredPost 메서드에서 log 테이블에 존재하지 않는 posts를 추천하기 때문
        if (!logs.isEmpty()) {
            log.error("기술 블로그 추천 후 로깅이 제대로 동작하지 않습니다.");
            log.error("memberId -> " + memberId);
            throw LoggingExceptionCode.INVALID_RECOMMEND_POSTS_LOGGING.newInstance();

        }

        //로그 데이터 INSERT
        insertMemberPostLog(memberId, postIds);
    }

    public void insertMemberPostLog(final Long memberId, final List<Long> postIds) {
        final Set<Long> postIdSet = new HashSet<>(postIds);

        //추천 상태를 true로 설정, 읽음 상태는 false인 이유는 추천할 때 읽지 않은 것을 기준으로 추천을 해주기 때문
        List<MemberPostLog> insertList = postIdSet.stream()
                .map(postId -> MemberPostLog.builder()
                        .postId(postId)
                        .memberId(memberId)
                        .isRead(false)
                        .isRecommended(true)
                        .build()).toList();

        memberPostLogRepository.saveAll(insertList);
    }

    public void insertMemberPostHistory(final Long memberId, final Long postId) {
        MemberPostHistory history = MemberPostHistory.builder()
                .postId(postId)
                .memberId(memberId)
                .build();
        memberPostHistoryRepository.save(history);
    }

    private BooleanExpression postIdsInOrEq(final List<Long> postIds) {
        return postIds.size() == 1 ? memberPostLog.postId.eq(postIds.get(0))
                : memberPostLog.postId.in(postIds);
    }
}
