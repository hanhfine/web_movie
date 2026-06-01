package com.webphim.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seat_types")
public class SeatType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_type_id")
    private Integer seatTypeId;

    @Column(name = "Name", nullable = false, length = 50)
    private String name;

    @Column(name = "Surcharge", nullable = false, precision = 10, scale = 2)
    private BigDecimal surcharge = BigDecimal.ZERO;

    @OneToMany(mappedBy = "seatType")
    private List<Seat> seats = new ArrayList<>();

    public Integer getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getSurcharge() { return surcharge == null ? BigDecimal.ZERO : surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}
