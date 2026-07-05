package com.ffs.server.service;

import com.ffs.server.mapper.ApplicationMapper;
import com.ffs.server.model.entity.Application;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationMapper applicationMapper;

    public ApplicationService(ApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    public Application create(String name, String description) {
        Application app = new Application(name, description);
        applicationMapper.insert(app);
        return app;
    }

    public List<Application> listAll() {
        return applicationMapper.findAll();
    }

    public Application getById(Long id) {
        Application app = applicationMapper.findById(id);
        if (app == null) throw new RuntimeException("Application not found: " + id);
        return app;
    }

    public Application getByName(String name) {
        Application app = applicationMapper.findByName(name);
        if (app == null) throw new RuntimeException("Application not found: " + name);
        return app;
    }
}
