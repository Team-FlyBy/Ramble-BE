package com.flyby.ramble.report.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_reports")
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

    @ManyToOne
    @Column(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @ManyToOne
    @Column(name = "reporting_user_id", nullable = false)
    private User reportingUser;

    @ManyToOne
    @Column(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Builder
    public UserReport(User reportedUser, User reportingUser, Session session, ReportReason reason, String detail) {
        this.externalId = UUID.randomUUID();
        this.reportedUser = reportedUser;
        this.reportingUser = reportingUser;
        this.session = session;
        this.reason = reason;
        this.detail = detail;
    }
}
