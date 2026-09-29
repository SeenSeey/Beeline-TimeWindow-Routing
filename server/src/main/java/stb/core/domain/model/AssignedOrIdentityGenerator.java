package stb.core.domain.model;

import java.util.Properties;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.mapping.SimpleValue;

public class AssignedOrIdentityGenerator extends IdentityGenerator implements BeforeExecutionGenerator {

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties parameters) {
        super.configure(creationContext, parameters);
        ((SimpleValue) creationContext.getValue()).setColumnToIdentity();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return currentValue;
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }

    @Override
    public boolean generatedOnExecution(Object owner, SharedSessionContractImplementor session) {
        return ((BaseEntity) owner).getId() == 0;
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
