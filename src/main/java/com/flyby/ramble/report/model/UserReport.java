package com.flyby.ramble.report.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 사용자 신고 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_reports",
        indexes = {
                @Index(name = "idx_reported_user_and_status", columnList = "reported_user_id, status")
        }
)
public class UserReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "external_id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false,
            unique = true)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_user_id", nullable = false)
    private User reportingUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserReportStatus status;

    @Builder
    public UserReport(User reportedUser, User reportingUser, Session session,
                      ReportReason reason, String detail, UserReportStatus status) {
        this.externalId = UUID.randomUUID();
        this.reportedUser = reportedUser;
        this.reportingUser = reportingUser;
        this.session = session;
        this.reason = reason;
        this.detail = detail;
        this.status = status;
    }

}

