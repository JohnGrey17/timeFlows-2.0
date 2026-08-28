package example.timeflows.service;

import example.timeflows.exception.DivisionException;
import example.timeflows.model.Division;
import example.timeflows.model.Subdivision;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.SubdivisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubdivisionServiceImpl implements SubdivisionService {

    private final SubdivisionRepository subdivisions;
    private final DivisionRepository divisions;

    public SubdivisionServiceImpl(
            SubdivisionRepository subdivisions, DivisionRepository divisions) {
        this.subdivisions = subdivisions;
        this.divisions = divisions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subdivision> findAll() {
        return subdivisions.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subdivision> findByDivision(Long divisionId) {
        return subdivisions.findByDivisionIdOrderByNameAsc(divisionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subdivision findById(Long id) {
        return subdivisions
                .findById(id)
                .orElseThrow(() -> new DivisionException("Підвідділ не знайдено"));
    }

    @Override
    @Transactional
    public Subdivision create(String name, String description, Long divisionId) {
        if (name == null || name.isBlank()) {
            throw new DivisionException("Назва підвідділу обов'язкова");
        }
        String normalizedName = name.trim();
        Division division =
                divisions
                        .findById(divisionId)
                        .orElseThrow(() -> new DivisionException("Відділ не знайдено"));
        if (subdivisions.existsByDivisionIdAndNameIgnoreCase(divisionId, normalizedName)) {
            throw new DivisionException("Підвідділ з такою назвою вже існує");
        }
        Subdivision subdivision = new Subdivision();
        subdivision.setName(normalizedName);
        subdivision.setDescription(
                description == null || description.isBlank() ? null : description.trim());
        subdivision.setDivision(division);
        return subdivisions.save(subdivision);
    }

    @Override
    @Transactional
    public Subdivision update(Long id, String name, String description, Long divisionId) {
        Subdivision subdivision = findById(id);
        if (name == null || name.isBlank()) {
            throw new DivisionException("Назва підвідділу обов'язкова");
        }
        Division division =
                divisions
                        .findById(divisionId)
                        .orElseThrow(() -> new DivisionException("Відділ не знайдено"));
        String normalizedName = name.trim();
        if ((!subdivision.getDivision().getId().equals(divisionId)
                        || !subdivision.getName().equalsIgnoreCase(normalizedName))
                && subdivisions.existsByDivisionIdAndNameIgnoreCase(divisionId, normalizedName)) {
            throw new DivisionException("Підвідділ з такою назвою вже існує");
        }
        subdivision.setName(normalizedName);
        subdivision.setDescription(
                description == null || description.isBlank() ? null : description.trim());
        subdivision.setDivision(division);
        return subdivisions.save(subdivision);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subdivision subdivision = findById(id);
        if (subdivisions.existsByIdAndUsersActiveTrue(id)) {
            throw new DivisionException("Не можна видалити підвідділ з активними працівниками");
        }
        subdivisions.delete(subdivision);
    }
}
