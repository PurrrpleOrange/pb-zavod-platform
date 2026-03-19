package com.pb.booking.api.controller;

import com.pb.booking.api.dto.request.CreateClientRequest;
import com.pb.booking.api.dto.request.UpdateClientRequest;
import com.pb.booking.api.dto.response.ClientResponse;
import com.pb.booking.domain.entity.Client;
import com.pb.booking.mapper.ClientMapper;
import com.pb.booking.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        Client client = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientMapper.toResponse(client));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateClientRequest request) {
        Client client = clientService.updateClient(id, request);
        return ResponseEntity.ok(clientMapper.toResponse(client));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable("id") UUID id) {
        Client client = clientService.getClient(id);
        return ResponseEntity.ok(clientMapper.toResponse(client));
    }

    @GetMapping
    public ResponseEntity<Page<ClientResponse>> getClients(@PageableDefault(size = 20) Pageable pageable) {
        Page<ClientResponse> page = clientService.getClients(pageable)
                .map(clientMapper::toResponse);
        return ResponseEntity.ok(page);
    }
}
