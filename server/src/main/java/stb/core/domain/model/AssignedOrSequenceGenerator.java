package stb.core.domain.model;

import java.util.Properties;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

public class AssignedOrSequenceGenerator extends SequenceStyleGenerator {

    private final String sequenceName;

    public AssignedOrSequenceGenerator(AssignedOrSequence config) {
        this.sequenceName = config.sequenceName();
    }

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties parameters) {
        parameters.setProperty(SEQUENCE_PARAM, sequenceName);
        super.configure(creationContext, parameters);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        if (currentValue instanceof Integer id && id != 0) {
            return id;
        }
        return super.generate(session, owner, currentValue, eventType);
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
