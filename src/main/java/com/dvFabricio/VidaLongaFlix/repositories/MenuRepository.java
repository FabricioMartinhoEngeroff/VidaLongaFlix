package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MenuRepository extends JpaRepository<Menu, UUID> {
}
