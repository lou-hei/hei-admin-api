package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.Place;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.model.validator.EventValidator;
import school.hei.haapi.repository.EventRepository;
import school.hei.haapi.repository.PlaceRepository;
import school.hei.haapi.repository.UserRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class EventService {
    private final EventValidator eventValidator;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    public Event getById(String eventId){
        return eventRepository.getById(eventId);
    }

    public List<Event> getAll(){
        return eventRepository.findAll();
    }

    @Transactional
    public Event accept(Event event){
        eventValidator.accept(event);
        User user = userRepository
                .findById(event.getSupervisor().getId())
                .orElseThrow(() ->
                        new BadRequestException("User with id "+event
                                .getSupervisor()
                                .getId()+"doesn't exist"));
        Place place = placeRepository
                .findById(event.getPlace().getId())
                .orElseThrow(() ->
                    new BadRequestException("Place with id "+event
                            .getPlace()
                            .getId()+"doesn't exist")
                );
        event.setSupervisor(user);
        event.setPlace(place);
        return event;
    }

    @Transactional
    public List<Event> saveAll(List<Event> events){
        List<Event> saved = new ArrayList<>();
        events.forEach(event -> saved.add(accept(event)));
        eventValidator.accept(saved);
        return eventRepository.saveAll(saved);
    }
}
