package com.pb.booking.service;

import com.pb.booking.api.dto.request.CreateClientRequest;
import com.pb.booking.api.dto.request.UpdateClientRequest;
import com.pb.booking.domain.entity.Client;
import com.pb.booking.exception.BookingException;
import com.pb.booking.exception.ErrorCode;
import com.pb.booking.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public Client createClient(CreateClientRequest request) {
        Client client = Client.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        client = clientRepository.save(client);
        log.info("Client created: id={}, name={}", client.getId(), client.getName());
        return client;
    }

    @Transactional
    public Client updateClient(UUID clientId, UpdateClientRequest request) {
        Client client = findClientOrThrow(clientId);

        if (request.getName() != null) {
            client.setName(request.getName());
        }
        if (request.getPhone() != null) {
            client.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            client.setEmail(request.getEmail());
        }

        client = clientRepository.save(client);
        log.info("Client updated: id={}", clientId);
        return client;
    }

    @Transactional(readOnly = true)
    public Client getClient(UUID clientId) {
        return findClientOrThrow(clientId);
    }

    @Transactional(readOnly = true)
    public Page<Client> getClients(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    private Client findClientOrThrow(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new BookingException(ErrorCode.BOOKING_CLIENT_INVALID,
                        "Client not found: " + clientId));
    }
}
