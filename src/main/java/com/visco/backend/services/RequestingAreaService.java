package com.visco.backend.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.repositories.RequestingAreaRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestingAreaService {

	private final RequestingAreaRepository requestingAreaRepository;

	@Transactional(readOnly = true)
	public Page<RequestingArea> getAllAreas(Pageable pageable) {
		return requestingAreaRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public RequestingArea getAreaById(Long id) {
		return requestingAreaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Requesting area not found: " + id));
	}

	@Transactional
	public RequestingArea createArea(RequestingArea area) {
		if (requestingAreaRepository.findByName(area.getName()).isPresent()) {
			throw new IllegalArgumentException("Area with name '" + area.getName() + "' already exists");
		}
		if (requestingAreaRepository.findByCostCenter(area.getCostCenter()).isPresent()) {
			throw new IllegalArgumentException("Area with cost center '" + area.getCostCenter() + "' already exists");
		}
		area.setActive(true);
		return requestingAreaRepository.save(area);
	}

	@Transactional
	public RequestingArea updateArea(Long id, RequestingArea updated) {
		RequestingArea existing = requestingAreaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Requesting area not found: " + id));

		if (!existing.getName().equals(updated.getName())
				&& requestingAreaRepository.findByName(updated.getName()).isPresent()) {
			throw new IllegalArgumentException("Area with name '" + updated.getName() + "' already exists");
		}
		if (!existing.getCostCenter().equals(updated.getCostCenter())
				&& requestingAreaRepository.findByCostCenter(updated.getCostCenter()).isPresent()) {
			throw new IllegalArgumentException("Area with cost center '" + updated.getCostCenter() + "' already exists");
		}

		existing.setName(updated.getName());
		existing.setDescription(updated.getDescription());
		existing.setCostCenter(updated.getCostCenter());
		return requestingAreaRepository.save(existing);
	}

	@Transactional
	public void deactivateArea(Long id) {
		RequestingArea area = requestingAreaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Requesting area not found: " + id));
		area.setActive(false);
		requestingAreaRepository.save(area);
	}

	@Transactional
	public void activateArea(Long id) {
		RequestingArea area = requestingAreaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Requesting area not found: " + id));
		area.setActive(true);
		requestingAreaRepository.save(area);
	}
}
