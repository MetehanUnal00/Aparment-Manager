package com.example.apartmentmanagerapi.config;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;

/**
 * Custom H2 dialect that disables the use of RETURNING clause for identity columns.
 * This is necessary because H2 doesn't support PostgreSQL's RETURNING syntax.
 */
public class H2DialectCustom extends H2Dialect {
    
    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new H2IdentityColumnSupport() {
            @Override
            public boolean supportsInsertSelectIdentity() {
                // Disable the use of insert...returning syntax
                return false;
            }
            
            @Override
            public String getIdentityInsertString() {
                // Return null to force the use of getGeneratedKeys() instead
                return null;
            }
            
            // Removed getIdentitySelectString override as it doesn't exist in parent
            // The parent class already handles this correctly for H2
            
            @Override
            public boolean hasDataTypeInIdentityColumn() {
                // H2 requires the data type in identity column definition
                return true;
            }
        };
    }
    
    @Override
    public boolean supportsInsertReturning() {
        // Explicitly disable INSERT...RETURNING support
        return false;
    }
    
    @Override
    public boolean supportsInsertReturningGeneratedKeys() {
        // Disable any form of RETURNING clause
        return false;
    }
}