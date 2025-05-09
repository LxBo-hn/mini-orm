package com.t2308e.core;

import com.t2308e.config.DataSourceConfig;
import com.t2308e.exception.MiniOrmException;
import com.t2308e.repository.MyCrudRepository;

import java.lang.reflect.Proxy;

public class MyRepositoryFactory {
    private final DataSourceConfig dataSourceConfig;

    public MyRepositoryFactory(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    @SuppressWarnings("unchecked")
    public <T, ID, R extends MyCrudRepository<T, ID>> R createRepository(Class<R> repositoryInterface) {
        if (!repositoryInterface.isInterface()) {
            throw new MiniOrmException("Provided class " + repositoryInterface.getName() + " is not an interface.");
        }

        // Check if it extends MyCrudRepository (or is MyCrudRepository itself)
        boolean extendsMyCrud = false;
        for (Class<?> interf : repositoryInterface.getInterfaces()) {
            if (MyCrudRepository.class.isAssignableFrom(interf)) {
                // This check is tricky due to generics. We actually need to check if
                // MyCrudRepository is a superinterface of one of the repositoryInterface's direct superinterfaces.
                // A simpler check is if the interface directly extends MyCrudRepository.
                // For a robust check, one might need to recursively check superinterfaces.
                // Let's assume for simplicity it directly extends MyCrudRepository or MyCrudRepository is in its hierarchy.
                extendsMyCrud = true;
                break;
            }
        }
        if (!MyCrudRepository.class.isAssignableFrom(repositoryInterface) && !extendsMyCrud) {
            // This check might be too strict if MyCrudRepository is not a direct superinterface.
            // A more common scenario is repositoryInterface EXTENDS MyCrudRepository.
            // Let's check if any of its direct interfaces is MyCrudRepository
            boolean found = false;
            for(Class<?> i : repositoryInterface.getInterfaces()){
                if(i.equals(MyCrudRepository.class)){
                    found = true;
                    break;
                }
            }
            if(!found) {
                throw new MiniOrmException(repositoryInterface.getName() + " must extend MyCrudRepository.");
            }
        }


        return (R) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                new RepositoryInvocationHandler<>(dataSourceConfig, repositoryInterface)
        );
    }
}

