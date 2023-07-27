package school.hei.haapi.endpoint.rest.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.TranscriptVersion;

@Component
@AllArgsConstructor
public class TranscriptVersionMapper {
    public TranscriptVersion toRest(school.hei.haapi.model.TranscriptVersion version) {
        return new TranscriptVersion()
                .id(version.getId())
                .ref(version.getRef())
                .creationDatetime(version.getCreationDatetime())
                .transcriptId(version.getTranscript().getId())
                .userId(version.getUser().getId());
    }
    public TranscriptVersion toRestPdf(school.hei.haapi.model.TranscriptVersion version) {
        return new TranscriptVersion()
                .id(version.getId())
                .ref(version.getRef())
                .pdfLink(version.getPdf_link())
                .creationDatetime(version.getCreationDatetime())
                .transcriptId(version.getTranscript().getId())
                .userId(version.getUser().getId());
    }
}
