/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.dataservice.jdbc;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaDate;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author nhudak
 */
@RunWith( MockitoJUnitRunner.class )
public abstract class BaseResultSetTest extends JDBCTestBase<BaseResultSet> {
  public static final String INVALID_COLUMN_REFERENCE = "Invalid column reference: ";
  @Spy RowMeta rowMeta;

  public final ImmutableMap<String, Method> STATES = ImmutableMap.of(
    "beforeFirst", getMethod( "isBeforeFirst" ),
    "first", getMethod( "isFirst" ),
    "last", getMethod( "isLast" ),
    "afterLast", getMethod( "isAfterLast" )
  );

  public BaseResultSetTest( Class<? extends BaseResultSet> type ) {
    super( type );
  }

  protected Map<String, Boolean> getState() {
    return Maps.transformValues( STATES, new Function<Method, Boolean>() {
      @Override public Boolean apply( Method input ) {
        try {
          return (Boolean) invoke( getTestObject(), input );
        } catch ( Exception e ) {
          throw new AssertionError( e );
        }
      }
    } );
  }

  protected void verifyState( String... expectedStates ) throws Exception {
    Object[] expected = FluentIterable.from( STATES.keySet() )
      .toMap( Functions.forPredicate( Predicates.in( ImmutableSet.copyOf( expectedStates ) ) ) )
      .entrySet()
      .toArray();
    assertThat( "ResultSet state", getState().entrySet(), contains( expected ) );
  }

  abstract void setNextRow( Object[] nextRow );

  @Test public void testGetBoolean() throws Exception {
    setNextRow( new Object[] { false, "foo" } );
    rowMeta.addValueMeta( new ValueMetaBoolean( "bool" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getBoolean( "bool" ), equalTo( false ) );
  }

  @Test public void testGetNull() throws Exception {
    setNextRow( new Object[] { null } );
    rowMeta.addValueMeta( new ValueMetaString( "foo" ) );
    getTestObject().next();
    assertThat( getTestObject().getString( "foo" ), nullValue() );
    assertThat( getTestObject().wasNull(), is( true ) );
  }

  @Test public void testGetDate() throws Exception {
    long time = System.currentTimeMillis();
    setNextRow( new Object[] { new Date( time ) } );
    rowMeta.addValueMeta( new ValueMetaDate( "foo" ) );
    getTestObject().next();
    assertThat( getTestObject().getDate( "foo" ), equalTo( new java.sql.Date( time ) ) );
    assertThat( getTestObject().getTimestamp( "foo" ), equalTo( new java.sql.Timestamp( time ) ) );
    assertThat( getTestObject().getTime( "foo" ), equalTo( new java.sql.Time( time ) ) );
  }

  @Test public void testGetDouble() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getDouble( "number" ), equalTo( 1.1 ) );
    assertThat( getTestObject().getFloat( "number" ), equalTo( 1.1f ) );
  }

  @Test public void testGetBigDecimal() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getBigDecimal( "number" ).doubleValue(), equalTo( 1.1 ) );
  }

  @Test public void testGetByte() throws Exception {
    byte b = 'a';
    setNextRow( new Object[] { new Byte( b ).longValue(), "foo" } );
    rowMeta.addValueMeta( new ValueMetaInteger( "byte" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getByte( "byte" ), equalTo( b ) );
  }

  @Test public void testGetBytes() throws Exception {
    setNextRow( new Object[] { "b", "foo" } );
    rowMeta.addValueMeta( new ValueMetaString( "bytes" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getBytes( "bytes" ), equalTo( new byte[] { 'b' } ) );

  }

  @Test public void testGetFloat() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "col" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getFloat( "col" ), equalTo( 1.1f ) );
  }

  @Test public void testGetInt() throws Exception {
    setNextRow( new Object[] { 1l, "foo" } );
    rowMeta.addValueMeta( new ValueMetaInteger( "col" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getInt( "col" ), equalTo( 1 ) );

  }

  @Test public void testGetLong() throws Exception {
    setNextRow( new Object[] { 1l, "foo" } );
    rowMeta.addValueMeta( new ValueMetaInteger( "col" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getLong( "col" ), equalTo( 1l ) );
    assertThat( getTestObject().getInt( "col" ), equalTo( 1 ) );
  }

  @Test public void testGetShort() throws Exception {
    setNextRow( new Object[] { 1l, "foo" } );
    rowMeta.addValueMeta( new ValueMetaInteger( "col" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    short s = 1;
    getTestObject().next();
    assertThat( getTestObject().getShort( "col" ), equalTo( s ) );

  }

  @Test public void testGetString() throws Exception {
    setNextRow( new Object[] { "a string", "foo" } );
    rowMeta.addValueMeta( new ValueMetaString( "col" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    assertThat( getTestObject().getString( "col" ), equalTo( "a string" ) );
    assertThat( getTestObject().getObject( "col", String.class ), equalTo( "a string" ) );
  }

  @Test
  public void testGetColumnNotPresentDouble() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    try {
      getTestObject().getDouble( "nonesuch" );
    } catch ( SQLException e ) {
      assertThat( e.getMessage(), equalTo( INVALID_COLUMN_REFERENCE + "nonesuch" ) );
      return;
    }
    fail();
  }

  @Test
  public void testGetColumnNotPresentString() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    try {
      getTestObject().getString( "nonesuch" );
    } catch ( SQLException e ) {
      assertThat( e.getMessage(), equalTo( INVALID_COLUMN_REFERENCE + "nonesuch" ) );
      return;
    }
    fail();
  }

  @Test
  public void testGetColumnIndexNotPresentString() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    try {
      getTestObject().getString( 4 );
    } catch ( SQLException e ) {
      assertThat( e.getMessage(), equalTo( INVALID_COLUMN_REFERENCE + 4 ) );
      return;
    }
    fail();
  }

  @Test
  public void testGetColumnNotPresentObject() throws Exception {
    setNextRow( new Object[] { 1.1, "foo" } );
    rowMeta.addValueMeta( new ValueMetaNumber( "number" ) );
    rowMeta.addValueMeta( new ValueMetaString( "string" ) );
    getTestObject().next();
    try {
      getTestObject().getObject( "nonesuch" );
    } catch ( SQLException e ) {
      assertThat( e.getMessage(), equalTo( INVALID_COLUMN_REFERENCE + "nonesuch" ) );
      return;
    }
    fail();
  }
}
