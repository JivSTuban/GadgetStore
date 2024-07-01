package com.tuban.GadgetStore.Services;

import com.tuban.GadgetStore.Entities.ProductsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<ProductsEntity, Integer> {
}
