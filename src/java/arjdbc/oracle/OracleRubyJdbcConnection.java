/*
 **** BEGIN LICENSE BLOCK *****
 * Copyright (c) 2006-2010 Nick Sieger <nick@nicksieger.com>
 * Copyright (c) 2006-2007 Ola Bini <ola.bini@gmail.com>
 * Copyright (c) 2008-2009 Thomas E Enebo <enebo@acm.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***** END LICENSE BLOCK *****/
package arjdbc.oracle;

import arjdbc.jdbc.RubyJdbcConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author nicksieger
 */
public class OracleRubyJdbcConnection extends RubyJdbcConnection {
    
    protected OracleRubyJdbcConnection(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }
    
    public static RubyClass createOracleJdbcConnectionClass(Ruby runtime, RubyClass jdbcConnection) {
        final RubyClass clazz = RubyJdbcConnection.getConnectionAdapters(runtime).
            defineClassUnder("OracleJdbcConnection", jdbcConnection, ORACLE_JDBCCONNECTION_ALLOCATOR);
        clazz.defineAnnotatedMethods(OracleRubyJdbcConnection.class);
        return clazz;
    }

    private static ObjectAllocator ORACLE_JDBCCONNECTION_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new OracleRubyJdbcConnection(runtime, klass);
        }
    };

    /**
     * Oracle needs this override to reconstruct NUMBER which is different
     * from NUMBER(x) or NUMBER(x,y).
     */
    @Override
    protected String typeFromResultSet(final ResultSet resultSet) throws SQLException {
        int precision = intFromResultSet(resultSet, COLUMN_SIZE);
        int scale = intFromResultSet(resultSet, DECIMAL_DIGITS);

        // According to http://forums.oracle.com/forums/thread.jspa?threadID=658646
        // Unadorned NUMBER reports scale == null, so we look for that here.
        if ( scale < 0 && resultSet.getInt(DATA_TYPE) == java.sql.Types.DECIMAL ) {
            precision = -1;
        }

        final String type = resultSet.getString(TYPE_NAME);
        return formatTypeWithPrecisionAndScale(type, precision, scale);
    }
    
    @Override
    protected RubyArray mapTables(final Ruby runtime, final DatabaseMetaData metaData, 
            final String catalog, final String schemaPattern, final String tablePattern, 
            final ResultSet tablesSet) throws SQLException {
        final List<IRubyObject> tables = new ArrayList<IRubyObject>(32);
        while ( tablesSet.next() ) {
            String name = tablesSet.getString(TABLES_TABLE_NAME);
            name = caseConvertIdentifierForRails(metaData, name);
            // Handle stupid Oracle 10g RecycleBin feature
            if ( name.startsWith("bin$") ) continue;
            tables.add(RubyString.newUnicodeString(runtime, name));
        }
        return runtime.newArray(tables);
    }
    
}
