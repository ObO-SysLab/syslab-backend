package net.diveon.backend.domain.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ProblemCreatePracticeRequest {
    /*
    실습형 문제 생성의 경우, 현재는 파일 업로드(.zip)가 필요해서 multipart/form-data형식으로만 가능.
    근데 이 형식은 @ModelAttribute로 받는데, 이건 @JsonProperty 무시하고 Java 필드명 그대로 매핑
    그래서 osImage, allowedCommands, cpuLimit, memoryLimit 전부 camelCase로 보내야 함
    다른 API들은 JSON이라 snake_case인데 이 API만 예외

    -> 이후 zip파일을 직접 올리는 방식이아니라, 자동 생성 방식으로 고도화 되면 아마 form-data형식을 버리게 될 거 같아서
       그땐 다시 snake_case로 가능할듯합니다.

    */
    

    @NotBlank
    private String title;

    @NotBlank
    private String summary;

    @NotBlank
    private String description;

    @NotBlank
    private String category;

    @NotBlank
    private String difficulty;

    @NotBlank
    private String visibility;

    @NotBlank
    private String osImage;

    private List<String> allowedCommands;

    private String cpuLimit = "0.3";

    private String memoryLimit = "256m";

    @NotBlank
    private String flag;

    private String hint;

    private Integer timeLimitSec;

    @NotNull
    private MultipartFile dockerfile;

    public ProblemCreatePracticeRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getOsImage() { return osImage; }
    public void setOsImage(String osImage) { this.osImage = osImage; }

    public List<String> getAllowedCommands() { return allowedCommands; }
    public void setAllowedCommands(List<String> allowedCommands) { this.allowedCommands = allowedCommands; }

    public String getCpuLimit() { return cpuLimit; }
    public void setCpuLimit(String cpuLimit) { this.cpuLimit = cpuLimit; }

    public String getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(String memoryLimit) { this.memoryLimit = memoryLimit; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public Integer getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(Integer timeLimitSec) { this.timeLimitSec = timeLimitSec; }

    public MultipartFile getDockerfile() { return dockerfile; }
    public void setDockerfile(MultipartFile dockerfile) { this.dockerfile = dockerfile; }
}
