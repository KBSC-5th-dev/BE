package kbsc.greenFunding.repository;

import kbsc.greenFunding.entity.UpcyclingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpcyclingOrderItemJpaRepository extends JpaRepository<UpcyclingOrderItem, Long> {
}