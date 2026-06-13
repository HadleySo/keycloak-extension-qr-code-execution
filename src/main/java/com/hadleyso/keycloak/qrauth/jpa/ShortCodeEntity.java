package com.hadleyso.keycloak.qrauth.jpa;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "AO_HADLEYSO_QR_SHORTCODE")
public class ShortCodeEntity {
    @Id
    @Column(name = "CODE", length = 9, nullable = false)
    protected String code;

    @Column(name = "REALM_ID", nullable = false)
    protected String realmId;

    @Column(name = "QR_VALUE", nullable = false)
    protected String qrValue;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT")
    protected Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = new Date();
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setQrValue(String qrValue) {
        this.qrValue = qrValue;
    }

    public String getQrValue() {
        return qrValue;
    }

    @Override
    public String toString() {
        return "ShortCodeEntity{" +
            "code=" + code +
            ", createdAt=" + createdAt +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof ShortCodeEntity))
            return false;

        ShortCodeEntity that = (ShortCodeEntity) o;

        if (!code.equals(that.code))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}