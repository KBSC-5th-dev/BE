package kbsc.greenFunding.service;

import kbsc.greenFunding.dto.project.ProjectDonationInfoRes;
import kbsc.greenFunding.dto.project.ProjectInfoReq;
import kbsc.greenFunding.dto.project.ProjectPlanReq;
import kbsc.greenFunding.dto.project.ProjectTypeReq;
import kbsc.greenFunding.dto.response.ErrorCode;
import kbsc.greenFunding.entity.*;
import kbsc.greenFunding.exception.NoEnumException;
import kbsc.greenFunding.repository.DonationJpaRepository;
import kbsc.greenFunding.repository.ImageJpaRepository;
import kbsc.greenFunding.repository.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    private final ProjectJpaRepository projectJpaRepo;
    private final DonationJpaRepository donationJpaRepo;
    private final ImageJpaRepository imageJpaRepo;


    // 프로젝트 type, category 저장
    @Transactional(rollbackFor=Exception.class)
    public Long postProjectType(Long userId, ProjectTypeReq projectTypeReq) {
        try {
            // User user = userJpaRepo.findById(userId).orElseThrow();

            Project project = Project.projectTypeBuilder()
                    .projectType(ProjectType.valueOf(projectTypeReq.getProjectType()))
                    .category(MaterialCategory.valueOf(projectTypeReq.getCategory()))
                    .build();

            Project projectId = projectJpaRepo.save(project);

            return projectId.getId();
        } catch(IllegalArgumentException e) {
            throw new NoEnumException("no enum", ErrorCode.NO_ENUM_CONSTANT);
        }
    }

    // 프로젝트 info 저장
    @Transactional
    public Long postProjectInfo(ProjectInfoReq projectInfoReq, String imageUrl, Long projectId) {
        Project project = projectJpaRepo.findById(projectId).orElseThrow();

        project.updateProjectInfo(projectInfoReq.getTitle(), imageUrl, projectInfoReq.getSummary());

       return project.getId();
    }

    // 프로젝트 plan 저장
    @Transactional
    public Long postProjectPlan(ProjectPlanReq projectPlanReq, Long projectId) {

        Project project = projectJpaRepo.findById(projectId).orElseThrow();

        if(project.getProjectType() == ProjectType.ALL) {
            // 기존 정보가 있는지 확인
            if(project.getDonation() != null) {
                project.getDonation().updateTotalWeight(projectPlanReq.getTotalWeight(), projectPlanReq.getTotalWeight());
            } else {
                Donation donation = Donation.donationBuilder()
                        .totalWeight(projectPlanReq.getTotalWeight())
                        .remainingWeight(projectPlanReq.getTotalWeight())
                        .build();

                donationJpaRepo.save(donation);
                project.setDonation(donation);
            }

            project.updateProjectPlan(projectPlanReq.getStartDate(), projectPlanReq.getEndDate());
            project.updateAmount(projectPlanReq.getAmount(), projectPlanReq.getAmount());
        } else if(project.getProjectType() == ProjectType.DONATION) {
            // 기존 정보가 있는지 확인
            if(project.getDonation() != null) {
                project.getDonation().updateTotalWeight(projectPlanReq.getTotalWeight(), projectPlanReq.getTotalWeight());
            } else {
                Donation donation = Donation.donationBuilder()
                        .totalWeight(projectPlanReq.getTotalWeight())
                        .remainingWeight(projectPlanReq.getTotalWeight())
                        .build();
                donationJpaRepo.save(donation);

                project.setDonation(donation);
            }

            project.updateProjectPlan(projectPlanReq.getStartDate(), projectPlanReq.getEndDate());
        } else {
            project.updateAmount(projectPlanReq.getAmount(), projectPlanReq.getAmount());
        }
        return project.getId();
    }

    @Transactional(rollbackFor=Exception.class)
    public Long postProjectContent (Long projectId, String content, List<String> fileUrlList) {
        Project project = projectJpaRepo.findById(projectId).orElseThrow();

        List<Image> imageEntityList = new ArrayList<Image>();

        // 이미지 파일 DB 관리
        fileUrlList.forEach(fileUrl -> {
            Image image = Image.builder()
                    .fileUrl(fileUrl)
                    .build();
            image.setProject(project);

            imageEntityList.add(image);
        });

        imageJpaRepo.saveAll(imageEntityList);

        project.updateContent(content);

        return project.getId();
    }

    @Transactional
    public void deleteProject(Long projectId) {
        projectJpaRepo.deleteById(projectId);
    }
}
