package com.visco.backend.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.RequestingAreaRepository;
import com.visco.backend.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final RequestingAreaRepository areaRepository;

	@Transactional(readOnly = true)
	public Page<UserDTO> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable)
				.map(UserDTO::fromUser);
	}

	@Transactional(readOnly = true)
	public UserDTO getUserById(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
		return UserDTO.fromUser(user);
	}

	@Transactional
	public UserDTO updateUser(UUID id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		user.setRole(request.role());

		if (request.areaId() != null) {
			RequestingArea area = areaRepository.findById(request.areaId())
					.orElseThrow(() -> new EntityNotFoundException("Area not found: " + request.areaId()));
			user.setArea(area);
		} else {
			user.setArea(null);
		}

		return UserDTO.fromUser(userRepository.save(user));
	}

	@Transactional
	public void deactivateUser(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
		user.setActive(false);
		userRepository.save(user);
	}

	@Transactional
	public void activateUser(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
		user.setActive(true);
		userRepository.save(user);
	}
}
