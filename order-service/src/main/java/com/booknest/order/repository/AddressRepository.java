package com.booknest.order.repository;

import com.booknest.order.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByAddressId(Long addressId);
    List<Address> findByCustomerId(Long customerId);
    List<Address> findByCity(String city);
    void deleteByCustomerId(Long customerId);
}
