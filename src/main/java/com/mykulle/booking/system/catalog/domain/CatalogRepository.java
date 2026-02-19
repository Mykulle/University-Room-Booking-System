package com.mykulle.booking.system.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository extends JpaRepository<CatalogRoom, Long> {

    List<CatalogRoom> findByProfileRoomType(CatalogRoom.RoomType roomType);

    List<CatalogRoom> findByOperationalStatus(CatalogRoom.OperationalStatus status);

    Optional<CatalogRoom> findByProfileRoomLocationValue(String value);}
