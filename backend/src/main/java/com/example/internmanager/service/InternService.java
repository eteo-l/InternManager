package com.example.internmanager.service;

import com.example.internmanager.dto.InternSubmissionPayload;
import com.example.internmanager.dto.InternSubmissionRequest;
import com.example.internmanager.dto.MentorUpdateRequest;
import com.example.internmanager.exception.ApiException;
import com.example.internmanager.model.FormStatus;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.model.ResourceStatus;
import com.example.internmanager.repository.InternRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InternService {

    private final InternRecordRepository internRecordRepository;

    public InternService(InternRecordRepository internRecordRepository) {
        this.internRecordRepository = internRecordRepository;
    }

    public synchronized List<InternRecord> list() {
        return internRecordRepository.findAll();
    }

    public synchronized InternRecord create(InternSubmissionRequest request) {
        InternRecord record = new InternRecord(
            UUID.randomUUID().toString(),
            request.name(),
            request.phone(),
            request.idNumber(),
            request.grade(),
            request.gender(),
            request.emergencyPhone(),
            request.school(),
            request.startDate(),
            request.endDate(),
            request.department(),
            request.campus(),
            request.mentor(),
            request.note(),
            FormStatus.PENDING,
            ResourceStatus.UNOPENED,
            ResourceStatus.UNOPENED,
            Instant.now()
        );

        internRecordRepository.save(record);
        return record;
    }

    public void validateRequest(InternSubmissionRequest request) {
        if (!request.phone().matches("^1\\d{10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写正确的实习生手机号");
        }

        if (!request.emergencyPhone().matches("^1\\d{10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写正确的紧急联系人手机号");
        }

        if (!request.idNumber().matches("^[0-9Xx]{15,18}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "身份证号格式不正确");
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "实习结束时间不能早于开始时间");
        }
    }

    public synchronized InternRecord update(String id, MentorUpdateRequest request) {
        InternRecord existing = getRequired(id);
        InternSubmissionPayload intern = request.intern();

        InternRecord updated = new InternRecord(
            existing.id(),
            intern.name(),
            intern.phone(),
            intern.idNumber(),
            intern.grade(),
            intern.gender(),
            intern.emergencyPhone(),
            intern.school(),
            intern.startDate(),
            intern.endDate(),
            intern.department(),
            intern.campus(),
            intern.mentor(),
            intern.note(),
            request.status(),
            request.accessStatus(),
            request.networkStatus(),
            Instant.now()
        );

        internRecordRepository.save(updated);
        return updated;
    }

    public synchronized InternRecord approve(String id) {
        InternRecord existing = getRequired(id);
        InternRecord updated = new InternRecord(
            existing.id(),
            existing.name(),
            existing.phone(),
            existing.idNumber(),
            existing.grade(),
            existing.gender(),
            existing.emergencyPhone(),
            existing.school(),
            existing.startDate(),
            existing.endDate(),
            existing.department(),
            existing.campus(),
            existing.mentor(),
            existing.note(),
            FormStatus.APPROVED,
            existing.accessStatus(),
            existing.networkStatus(),
            Instant.now()
        );

        internRecordRepository.save(updated);
        return updated;
    }

    public synchronized void reject(String id) {
        if (!internRecordRepository.deleteById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "实习生记录不存在");
        }
    }

    public synchronized void clear() {
        internRecordRepository.deleteAll();
    }

    private InternRecord getRequired(String id) {
        return internRecordRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "实习生记录不存在"));
    }
}
