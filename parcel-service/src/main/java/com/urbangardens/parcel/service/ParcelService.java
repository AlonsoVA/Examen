package com.urbangardens.parcel.service;

import com.urbangardens.parcel.dto.ParcelRequest;
import com.urbangardens.parcel.dto.ParcelResponse;

import java.util.List;

public interface ParcelService {

    ParcelResponse create(ParcelRequest request);

    List<ParcelResponse> findAll();

    ParcelResponse findById(Long id);
}
