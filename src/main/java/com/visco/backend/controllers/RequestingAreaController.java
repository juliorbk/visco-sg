package com.visco.backend.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.services.RequestingAreaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requesting-areas")
@RequiredArgsConstructor
public class RequestingAreaController {

	private final RequestingAreaService areaService;

	@GetMapping
	public ResponseEntity<Page<RequestingArea>> getAllAreas(Pageable pageable) {
		return ResponseEntity.ok(areaService.getAllAreas(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RequestingArea> getAreaById(@PathVariable Long id) {
		return ResponseEntity.ok(areaService.getAreaById(id));
	}

	@PostMapping
	public ResponseEntity<RequestingArea> createArea(@Valid @RequestBody RequestingArea area) {
		return ResponseEntity.status(HttpStatus.CREATED).body(areaService.createArea(area));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RequestingArea> updateArea(@PathVariable Long id, @Valid @RequestBody RequestingArea area) {
		return ResponseEntity.ok(areaService.updateArea(id, area));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivateArea(@PathVariable Long id) {
		areaService.deactivateArea(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/activate")
	public ResponseEntity<Void> activateArea(@PathVariable Long id) {
		areaService.activateArea(id);
		return ResponseEntity.noContent().build();
	}
}
