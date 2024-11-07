package school.hei.haapi.endpoint.rest.validator;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CrupdateGrade;

import java.util.Objects;
import java.util.function.Consumer;

@Component
@AllArgsConstructor
public class GradeValidator implements Consumer<CrupdateGrade> {
    @Override
    public void accept(CrupdateGrade crupdateGrade) {
        if (Objects.requireNonNull(crupdateGrade.getScore()) > 20 || Objects.requireNonNull(crupdateGrade.getScore()) < 0){
            throw new IllegalArgumentException("score must be between 0 and 20");
        }
    }
}
