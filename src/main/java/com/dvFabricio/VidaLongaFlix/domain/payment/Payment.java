//package com.dvFabricio.VidaLongaFlix.domain.payment;
//
//import com.dvFabricio.VidaLongaFlix.domain.user.User;
//import jakarta.persistence.*;
//import lombok.EqualsAndHashCode;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.util.Date;
//import java.util.UUID;
//
//@Entity
//@Table(name = "payments")
//@EqualsAndHashCode(of = "id")
//@Getter
//@Setter
//@NoArgsConstructor
//public class Payment {
//
//    @Id
//    @GeneratedValue
//    private UUID id;
//
//    @ManyToOne
//    private User user;
//
//    private String method;
//    private double amount;
//    private Date date;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus status;
//
//    private String externalTransactionId;
//    private String description;
//    private Date creationDate;
//    private Date updateDate;
//
//    public Payment(User user, String method, double amount, Date date, String description) {
//        this.user = user;
//        this.method = method;
//        this.amount = amount;
//        this.date = date;
//        this.description = description;
//        this.status = PaymentStatus.PENDING;
//        this.creationDate = new Date();
//        this.updateDate = new Date();
//    }
//}