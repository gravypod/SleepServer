/* 
 * Copyright (C) 2002-2012 Raphael Mudge (rsmudge@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package sleep.runtime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * <p>
 * A scalar is the universal data type for sleep variables. Scalars can have
 * numerical values of integer, double, or long. Scalars can have a string
 * value. Scalars can also contain a reference to a scalar array, scalar hash,
 * or a generic Java object.
 * 
 * <p>
 * Numerical and String values are stored as ScalarTypes. Arrays and Hashes are
 * stored in ScalarArray and ScalarHash containers respectively.
 * </p>
 * 
 * <h3>Instantiating a Scalar</h3>
 * 
 * <p>
 * Instantiating a Scalar is most easily done using the sleep.runtime.SleepUtils
 * class. The SleepUtils class contains several static methods for creating a
 * Scalar object from data.
 * </p>
 * 
 * <p>
 * The general pattern for this is a
 * {@link sleep.runtime.SleepUtils#getScalar(String) SleepUtils.getScalar(data)}
 * methods. There are static getScalar() methods that take a double, int, long,
 * Object, or a String as a parameter.
 * </p>
 * 
 * <p>
 * There are even methods for wrapping java data structures into a scalar array
 * or scalar hash. Methods also exist to copy data from one scalar into another
 * new scalar.
 * </p>
 * 
 * <p>
 * Examples:</b>
 * 
 * <pre>
 * Scalar anInt   = SleepUtils.getScalar(3); // create an int scalar
 * Scalar aDouble = SleepUtils.getScalar(4.5); // create a double scalar
 * Scalar aString = SleepUtils.getScalar("hello"); // string scalar
 * Scalar anArray = SleepUtils.getArrayWrapper(new LinkedList(); // array scalar
 * </pre>
 * 
 * <h3>Working with Scalar Arrays</h3>
 * 
 * <p>
 * To add a value to a Scalar array:
 * </p>
 * 
 * <pre>
 * Scalar arrayScalar = SleepUtils.getArray(); // empty array
 * arrayScalar.getArray().add(SleepUtils.getScalar(&quot;value&quot;), 0);
 * </pre>
 * 
 * <p>
 * To iterate through all of the values in a Scalar array:
 * </p>
 * 
 * <pre>
 * Iterator i = arrayScalar.getArray().scalarIterator();
 * while(i.hasNext()) {
 * 	Scalar temp = (Scalar) i.next();
 * }
 * </pre>
 * 
 * <h3>Working with Scalar Hashes</h3>
 * 
 * <p>
 * To add a value to a Scalar hashtable:
 * </p>
 * 
 * <pre>
 * Scalar hashScalar = SleepUtils.getHashScalar(); // blank hashtable
 * Scalar temp = hashScalar.getHash().getAt(SleepUtils.getScalar(&quot;key&quot;));
 * temp.setValue(SleepUtils.getScalar(&quot;value&quot;));
 * </pre>
 * 
 * <p>
 * The second line obtains a Scalar for "key". The returned Scalar is just a
 * container. It is possible to set the value of the returned scalar using the
 * setValue method.
 * </p>
 * 
 * <p>
 * Internally scalar values in sleep are passed by value. Methods like setValue
 * inside of the Scalar class take care of copying the value. Externally though
 * Scalar objects are passed by reference. When you call getAt() in the
 * ScalarHash you are obtaining a reference to a Scalar inside of the hashtable.
 * When you change the value of the Scalar you obtained, you change the value of
 * the Scalar in the hashtable.
 * </p>
 * 
 * @see sleep.runtime.SleepUtils
 * @see sleep.runtime.ScalarType
 * @see sleep.runtime.ScalarArray
 * @see sleep.runtime.ScalarHash
 */
public class Scalar implements Serializable {
	
	/**
     * 
     */
	private static final long serialVersionUID = -4060987368819590822L;
	
	protected ScalarType value = null;
	
	protected ScalarArray array = null;
	
	protected ScalarHash hash = null;
	
	/**
	 * Returns the actual non-array/non-hash value this scalar contains. This is
	 * mainly for use by internal sleep classes that do not want to accidentally
	 * convert a hash/array to a string.
	 */
	public ScalarType getActualValue() {
	
		return value;
	}
	
	/**
	 * Returns the container for the scalars value. If this is an array or hash
	 * scalar then they will be converted to a string scalar and returned. If
	 * this scalar is completely null then null will be returned which will mess
	 * up the interpreter somewhere
	 */
	public ScalarType getValue() {
	
		if (value != null) {
			return value;
		}
		
		/* these are in case the scalar is being misused */
		
		if (array != null) {
			return SleepUtils.getScalar(SleepUtils.describe(this)).getValue();
		}
		
		if (hash != null) {
			return SleepUtils.getScalar(SleepUtils.describe(this)).getValue();
		}
		
		return null;
	}
	
	/** the string value of this scalar */
	public String stringValue() {
	
		return getValue().toString();
	}
	
	/** the int value of this scalar */
	public int intValue() {
	
		return getValue().intValue();
	}
	
	/** the double value of this scalar */
	public double doubleValue() {
	
		return getValue().doubleValue();
	}
	
	/** the long value of this scalar */
	public long longValue() {
	
		return getValue().longValue();
	}
	
	/** the object value of this scalar */
	public Object objectValue() {
	
		if (array != null) {
			return array;
		}
		
		if (hash != null) {
			return hash;
		}
		
		return value.objectValue();
	}
	
	/**
	 * returns a scalar array referenced by this scalar iff this scalar contains
	 * an array reference
	 */
	public ScalarArray getArray() {
	
		return array;
	}
	
	/**
	 * returns a scalar hash referenced by this scalar iff this scalar contains
	 * a hash reference
	 */
	public ScalarHash getHash() {
	
		return hash;
	}
	
	/** set the value of this scalar container to a scalar value of some type */
	public void setValue(final ScalarType _value) {
	
		value = _value.copyValue();
		array = null;
		hash = null;
	}
	
	/** set the value of this scalar container to a scalar array */
	public void setValue(final ScalarArray _array) {
	
		value = null;
		array = _array;
		hash = null;
	}
	
	/** set the value of this scalar container to a scalar hash */
	public void setValue(final ScalarHash _hash) {
	
		value = null;
		array = null;
		hash = _hash;
	}
	
	/**
	 * returns an identity value for this scalar. the identity value is used in
	 * set operations. basically any scalar values that are handled by reference
	 * (object,s arrays, and hashes) use their reference as their identity.
	 * other values used their string value as their identity (doubles that do
	 * not have a decimal point will be converted to longs).
	 */
	public Object identity() {
	
		if (getArray() != null) {
			return array;
		}
		if (getHash() != null) {
			return hash;
		}
		if (value.getType() == sleep.engine.types.ObjectValue.class) {
			return objectValue();
		}
		return toString();
	}
	
	/**
	 * compares two scalars in terms of their identity. scalars that hold
	 * references (array, object, and hash) are compared by reference where
	 * other values are compared by their string value. doubles with a round
	 * value will be converted to a long
	 */
	public boolean sameAs(final Scalar other) {
	
		if (getArray() != null && other.getArray() != null && getArray() == other.getArray()) {
			return true;
		} else if (getHash() != null && other.getHash() != null && getHash() == other.getHash()) {
			return true;
		} else if (getActualValue() != null && other.getActualValue() != null) {
			if (getActualValue().getType() == sleep.engine.types.ObjectValue.class || other.getActualValue().getType() == sleep.engine.types.ObjectValue.class) {
				return objectValue() == other.objectValue();
			} else {
				return identity().equals(other.identity());
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
	
		return stringValue();
	}
	
	/**
	 * clones the value from the specified scalar and gives this scalar a copy
	 * of the value
	 */
	public void setValue(final Scalar newValue) {
	
		if (newValue == null) {
			return;
		}
		if (newValue.getArray() != null) {
			setValue(newValue.getArray());
			return;
		}
		if (newValue.getHash() != null) {
			setValue(newValue.getHash());
			return;
		}
		if (newValue.getValue() != null) {
			setValue(newValue.getValue());
			return;
		}
	}
	
	private void writeObject(final ObjectOutputStream out) throws IOException {
	
		if (SleepUtils.isEmptyScalar(this)) {
			out.writeObject(null);
		} else {
			out.writeObject(value);
		}
		out.writeObject(array);
		out.writeObject(hash);
	}
	
	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
	
		value = (ScalarType) in.readObject();
		array = (ScalarArray) in.readObject();
		hash = (ScalarHash) in.readObject();
		
		if (value == null && array == null && hash == null) {
			setValue(SleepUtils.getEmptyScalar());
		}
	}
}
