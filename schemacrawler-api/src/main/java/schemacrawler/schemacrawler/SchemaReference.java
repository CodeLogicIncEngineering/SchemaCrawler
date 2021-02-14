/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2021, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/
package schemacrawler.schemacrawler;

import static us.fatehi.utility.Utility.convertForComparison;
import static us.fatehi.utility.Utility.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;

public final class SchemaReference implements Schema {

  private static final long serialVersionUID = -5309848447599233878L;

  private final Map<String, Object> attributeMap = new HashMap<>();
  private final String catalogName;
  private final String schemaName;
  private transient String fullName;

  public SchemaReference() {
    this(null, null);
  }

  public SchemaReference(final String catalogName, final String schemaName) {
    this.catalogName = catalogName;
    this.schemaName = schemaName;
  }

  @Override
  public int compareTo(final NamedObject otherSchemaRef) {
    if (otherSchemaRef == null) {
      return -1;
    } else {
      return convertForComparison(getFullName())
          .compareTo(convertForComparison(otherSchemaRef.getFullName()));
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof SchemaReference)) {
      return false;
    }
    final Schema other = (Schema) obj;
    return Objects.equals(catalogName, other.getCatalogName())
        && Objects.equals(schemaName, other.getName());
  }

  /** {@inheritDoc} */
  @Override
  public <T> T getAttribute(final String name) {
    return getAttribute(name, null);
  }

  /** {@inheritDoc} */
  @Override
  public <T> T getAttribute(final String name, final T defaultValue) {
    final Object attributeValue = attributeMap.get(name);
    if (attributeValue == null) {
      return defaultValue;
    } else {
      try {
        return (T) attributeValue;
      } catch (final ClassCastException e) {
        return defaultValue;
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Object> getAttributes() {
    return Collections.unmodifiableMap(attributeMap);
  }

  @Override
  public String getCatalogName() {
    return catalogName;
  }

  @Override
  public String getFullName() {
    buildFullName();
    return fullName;
  }

  @Override
  public String getName() {
    return schemaName;
  }

  @Override
  public String getRemarks() {
    return "";
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasAttribute(final String name) {
    return attributeMap.containsKey(name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(catalogName, schemaName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasRemarks() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public <T> Optional<T> lookupAttribute(final String name) {
    return Optional.of(getAttribute(name));
  }

  /** {@inheritDoc} */
  @Override
  public void removeAttribute(final String name) {
    if (!isBlank(name)) {
      attributeMap.remove(name);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setAttribute(final String name, final Object value) {
    if (!isBlank(name)) {
      if (value == null) {
        attributeMap.remove(name);
      } else {
        attributeMap.put(name, value);
      }
    }
  }

  @Override
  public void setRemarks(final String remarks) {
    // No-op
  }

  @Override
  public String toString() {
    return getFullName();
  }

  @Override
  public List<String> toUniqueLookupKey() {
    return new ArrayList<>(Arrays.asList(catalogName, schemaName));
  }

  private void buildFullName() {
    if (fullName != null) {
      return;
    }

    final Identifiers identifiers =
        Identifiers.identifiers().withIdentifierQuoteString("\"").build();
    fullName = identifiers.quoteFullName(this);
  }
}
