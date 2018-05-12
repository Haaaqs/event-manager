package me.ihaq.eventmanager;

import me.ihaq.eventmanager.data.EventData;
import me.ihaq.eventmanager.listener.EventTarget;
import me.ihaq.eventmanager.listener.EventListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {

    //TODO -> Documentation

    private final Map<Class<? extends Event>, List<EventData>> registryMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void register(EventListener... eventListeners) {

        // looping through all the methods in the eventListeners and adding them to registryMap if they are valid
        Arrays.stream(eventListeners)
                .forEach(eventListener -> Arrays.stream(eventListener.getClass().getDeclaredMethods())
                        .filter(this::isMethodValid)
                        .forEach(method -> {

                            if (!method.isAccessible())
                                method.setAccessible(true);

                            Class<? extends Event> clazz = (Class<? extends Event>) method.getParameterTypes()[0];
                            EventData eventData = new EventData(eventListener, method, method.getAnnotation(EventTarget.class).value());

                            List<EventData> list = registryMap.getOrDefault(clazz, new CopyOnWriteArrayList<>());

                            if (!list.contains(eventData))
                                list.add(eventData);

                            registryMap.put(clazz, list);
                        }));

        // sorting the registry map
        registryMap.values()
                .forEach(eventDataList -> eventDataList.sort(((o1, o2) -> (o1.getPriority().getValue() - o2.getPriority().getValue()))));
    }

    public void unregister(EventListener... eventListeners) {
        Arrays.stream(eventListeners)
                .forEach(eventListener -> registryMap.values()
                        .forEach(eventDataList -> eventDataList.removeIf(field -> field.getEventListener() == eventListener)));

        // cleaning up the registryMap by removing any empty lists
        registryMap.entrySet()
                .removeIf(hashSetEntry -> hashSetEntry.getValue().isEmpty());
    }

    private boolean isMethodValid(Method method) {
        return method.getParameterTypes().length == 1 && method.isAnnotationPresent(EventTarget.class) && Event.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    public void callEvent(Event event) {
        List<EventData> dataList = registryMap.get(event.getClass());

        if (dataList == null)
            return;

        dataList.forEach(data -> {
            try {
                data.getMethod().invoke(data.getEventListener(), event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }
}