package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.IssueLabel;
import com.nexuspms.backlog.domain.Label;
import com.nexuspms.backlog.repository.IssueLabelRepository;
import com.nexuspms.backlog.repository.LabelRepository;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;

    public LabelService(LabelRepository labelRepository, IssueLabelRepository issueLabelRepository) {
        this.labelRepository = labelRepository;
        this.issueLabelRepository = issueLabelRepository;
    }

    @Transactional
    public Label create(UUID projectId, String name) {
        labelRepository.findByProjectIdAndName(projectId, name).ifPresent(l -> {
            throw new GovernanceSafeguardException("Label '" + name + "' already exists on this project.");
        });
        return labelRepository.save(new Label(projectId, name));
    }

    public List<Label> listForProject(UUID projectId) {
        return labelRepository.findByProjectId(projectId);
    }

    @Transactional
    public void delete(UUID labelId) {
        if (!labelRepository.existsById(labelId)) {
            throw new ResourceNotFoundException("Label " + labelId + " not found.");
        }
        labelRepository.deleteById(labelId);
    }

    @Transactional
    public void attach(UUID issueId, UUID labelId) {
        issueLabelRepository.save(new IssueLabel(issueId, labelId));
    }

    @Transactional
    public void detach(UUID issueId, UUID labelId) {
        issueLabelRepository.deleteByIssueIdAndLabelId(issueId, labelId);
    }

    public List<IssueLabel> listForIssue(UUID issueId) {
        return issueLabelRepository.findByIssueId(issueId);
    }
}
