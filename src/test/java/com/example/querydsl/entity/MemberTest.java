package com.example.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory ;

    @BeforeEach // 미리 실행됨
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }


     @Test // JPQL 방식
    public void startJpql() {

         String qlString =
                 "select m from Member m" +
                         " where m.username = :username";

         Member findMember = em.createQuery(qlString, Member.class)
                 .setParameter("username", "member1")
                 .getSingleResult();

         assertThat(findMember.getUsername()).isEqualTo("member1");
     }

     @Test // QueryDsl 방식 컴파일시 오류 찾음
    public void QueryDsl() {
       //  JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 필드로 뺌
        // QMember m = new QMember("m"); // 같은 테이블 조인할테
         //QMember member = QMember.member;

         Member findMember = queryFactory
                 .select(member)
                 .from(member)
                 .where(member.username.eq("member1")) // 파라미터 바인딩처리  자동 주입
                 .fetchOne();

         assertThat(findMember.getUsername()).isEqualTo("member1");

     }

     @Test // 검색조건
    public void search() {
         Member findMember = queryFactory
                 .selectFrom(member)
                 .where(member.username.eq("member1")
                         .and(member.age.between(10,30)))
                 .fetchOne();

         assertThat(findMember.getUsername()).isEqualTo("member1");

         member.username.eq("member1"); // username = 'member1'
         member.username.ne("member1"); //username != 'member1'
         member.username.eq("member1").not(); // username != 'member1'
         member.username.isNotNull(); //이름이 is not null
         member.age.in(10, 20); // age in (10,20)
         member.age.notIn(10, 20); // age not in (10, 20)
         member.age.between(10,30); //between 10, 30
         member.age.goe(30); // age >= 30
         member.age.gt(30); // age > 30
         member.age.loe(30); // age <= 30
         member.age.lt(30); // age < 301
         member.username.like("member%"); //like 검색
         member.username.contains("member"); // like ‘%member%’ 검색2
         member.username.startsWith("member"); //like ‘member%’ 검색1
     }


     @Test // And 사용법
    public void searchAndParam() {
         Member findMember = queryFactory
                 .selectFrom(member)
                 .where(
                         member.username.eq("member1"),  // and 만 있는경우 이방식이 더 좋음
                         member.age.eq(10)
                 )
                 .fetchOne();

         assertThat(findMember.getUsername()).isEqualTo("member1");

     }

     @Test // fetch
    public void resultFetch() {
         List<Member> fetch = queryFactory  // 리스트조회
                 .selectFrom(member)
                 .fetch();

         Member fetchOne = queryFactory  // 단건조회 결과없으면 null, 아니면 nom
                 .selectFrom(member)
                 .fetchOne();

         Member fetchFirst = queryFactory  // limit(1).fectone
                 .selectFrom(member)
                 .fetchFirst();

         QueryResults<Member> results = queryFactory // 페이징정보 포함, total count 쿼리 포함
                 .selectFrom(member)
                 .fetchResults();

         results.getTotal();
         List<Member> content = results.getResults();

         long total = queryFactory    // count 쿼리만 실행
                 .selectFrom(member)
                 .fetchCount();



     }

     /**
      * 회원 정렬순서
      * 1. 회원 내림차순(desc)
      * 2. 회원 이름 올림차순(asc)
      * 단 2에서 회원 이름이 없으면 마지막에 출력 ( nulls last )
      * */

     @Test // 정렬
    public  void sort () {
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

         List<Member> result = queryFactory
                 .selectFrom(member)
                 .where(member.age.eq(100))
                 .orderBy(member.age.desc(), member.username.asc().nullsLast()) // username 널이면 nullsLast, nullfirst 도 있음
                 .fetch();

         Member member5 = result.get(0);
         Member member6 = result.get(1);
         Member memberNull = result.get(2);
         assertThat(member5.getUsername()).isEqualTo("member5");
         assertThat(member6.getUsername()).isEqualTo("member6");
         assertThat(memberNull.getUsername()).isNull();
     }

     /**
      *  페이징 조회
      *  order by
      *   member0_.username desc limit ? offset ?
      * */

     @Test // 페이징
    public void paging() {
         List<Member> result = queryFactory
                 .selectFrom(member)
                 .orderBy(member.username.desc())
                 .offset(1)
                 .limit(2)
                 .fetch();

         assertThat(result.size()).isEqualTo(2);
     }

    /**
     *  페이징 조회 (fetchResults , 토탈카운트 까지 전체조회 )
     *  order by
     *   member0_.username desc limit ? offset ?
     * */

    @Test // 페이징
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test // 집합
    public void aggreation() {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     * 
     * .groupBy(item.price)
     * .having(item.price.gt(1000)
     * */

    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // 멤버에 있는 팀과 , 팀을 조인
                .groupBy(team.name) // 팀의 이름
               // .having(team.name.gt("memberA"))  having 기능 가능
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // 10 + 20 / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // 10 + 20 / 2
    }

    /**
     * 팀 A 에 소속된 모든 회원을 찾아라
     * letftjoin,rightjoin 다 가능
     */

    @Test // 조인
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // Qteam.team 임
                .where(team.name.eq("teamA"))
                .fetch();


        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");

    }

    /**
     * 세타 조인 ( 외부조인 불가능 )
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * from 절에 여러 엔티티를 선택해서 세타 조인
     */

    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // from 절 나열1
                .where(member.username.eq(team.name))
                .fetch();


        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }
}