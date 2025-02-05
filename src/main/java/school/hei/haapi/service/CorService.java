package school.hei.haapi.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Cor;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.repository.CorRepository;

@Service
@RequiredArgsConstructor
public class CorService {
  private final CorRepository corRepository;

  public List<Cor> getCorByStudentId(String studentId, PageFromOne page, BoundedPageSize size) {
    Pageable pageable =
        PageRequest.of(
            page.getValue() - 1, size.getValue(), Sort.by("creationDatetime").descending());
    return corRepository.findAllByConcernedStudentId(studentId, pageable);
  }

  public List<Cor> savaAllStudentCor(List<Cor> cors) {
    return corRepository.saveAll(cors);
  }
}
