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
package sleep.engine;

import java.util.List;

import sleep.engine.atoms.Assign;
import sleep.engine.atoms.AssignT;
import sleep.engine.atoms.Bind;
import sleep.engine.atoms.BindFilter;
import sleep.engine.atoms.BindPredicate;
import sleep.engine.atoms.Call;
import sleep.engine.atoms.Check;
import sleep.engine.atoms.CreateClosure;
import sleep.engine.atoms.CreateFrame;
import sleep.engine.atoms.Decide;
import sleep.engine.atoms.Get;
import sleep.engine.atoms.Goto;
import sleep.engine.atoms.Index;
import sleep.engine.atoms.Iterate;
import sleep.engine.atoms.ObjectAccess;
import sleep.engine.atoms.ObjectNew;
import sleep.engine.atoms.Operate;
import sleep.engine.atoms.PLiteral;
import sleep.engine.atoms.PopTry;
import sleep.engine.atoms.Return;
import sleep.engine.atoms.SValue;
import sleep.engine.atoms.Try;
import sleep.runtime.Scalar;

/**
 * A class providing methods for constructing an atomic step of a specific type.
 * Feel free to extend this class and specify your own factory to the
 * CodeGenerator class.
 */
public class GeneratedSteps {
	
	public Step PopTry() {
	
		final Step temp = new PopTry();
		return temp;
	}
	
	public Step Try(final Block owner, final Block handler, final String var) {
	
		final Step temp = new Try(owner, handler, var);
		return temp;
	}
	
	public Step Operate(final String oper) {
	
		final Step temp = new Operate(oper);
		return temp;
	}
	
	public Step Return(final int type) {
	
		final Step temp = new Return(type);
		return temp;
	}
	
	public Step SValue(final Scalar value) {
	
		final Step temp = new SValue(value);
		return temp;
	}
	
	public Step IteratorCreate(final String key, final String value) {
	
		return new Iterate(key, value, Iterate.ITERATOR_CREATE);
	}
	
	public Step IteratorNext() {
	
		return new Iterate(null, null, Iterate.ITERATOR_NEXT);
	}
	
	public Step IteratorDestroy() {
	
		return new Iterate(null, null, Iterate.ITERATOR_DESTROY);
	}
	
	public Check Check(final String nameOfOperator, final Block setupOperands) {
	
		final Check temp = new Check(nameOfOperator, setupOperands);
		return temp;
	}
	
	public Step Goto(final Check conditionForGoto, final Block ifTrue, final Block increment) {
	
		final Goto temp = new Goto(conditionForGoto);
		temp.setChoices(ifTrue);
		temp.setIncrement(increment);
		return temp;
	}
	
	public Step Decide(final Check conditionForGoto, final Block ifTrue, final Block ifFalse) {
	
		final Decide temp = new Decide(conditionForGoto);
		temp.setChoices(ifTrue, ifFalse);
		return temp;
	}
	
	public Step PLiteral(final List doit) {
	
		final Step temp = new PLiteral(doit);
		return temp;
	}
	
	public Step Assign(final Block variable) {
	
		final Step temp = new Assign(variable);
		return temp;
	}
	
	public Step AssignAndOperate(final Block variable, final String operator) {
	
		final Step temp = new Assign(variable, Operate(operator));
		return temp;
	}
	
	public Step AssignT() {
	
		final Step temp = new AssignT();
		return temp;
	}
	
	public Step AssignTupleAndOperate(final String operator) {
	
		final Step temp = new AssignT(Operate(operator));
		return temp;
	}
	
	public Step CreateFrame() {
	
		final Step temp = new CreateFrame();
		return temp;
	}
	
	public Step Get(final String value) {
	
		final Step temp = new Get(value);
		return temp;
	}
	
	public Step Index(final String value, final Block index) {
	
		final Step temp = new Index(value, index);
		return temp;
	}
	
	public Step Call(final String function) {
	
		final Step temp = new Call(function);
		return temp;
	}
	
	public Step CreateClosure(final Block code) {
	
		final Step temp = new CreateClosure(code);
		return temp;
	}
	
	public Step Bind(final String functionEnvironment, final Block name, final Block code) {
	
		final Step temp = new Bind(functionEnvironment, name, code);
		return temp;
	}
	
	public Step BindPredicate(final String functionEnvironment, final Check predicate, final Block code) {
	
		final Step temp = new BindPredicate(functionEnvironment, predicate, code);
		return temp;
	}
	
	public Step BindFilter(final String functionEnvironment, final String name, final Block code, final String filter) {
	
		final Step temp = new BindFilter(functionEnvironment, name, code, filter);
		return temp;
	}
	
	public Step ObjectNew(final Class<?> name) {
	
		final Step temp = new ObjectNew(name);
		return temp;
	}
	
	public Step ObjectAccess(final String name) {
	
		final Step temp = new ObjectAccess(name, null);
		return temp;
	}
	
	public Step ObjectAccessStatic(final Class<?> aClass, final String name) {
	
		final Step temp = new ObjectAccess(name, aClass);
		return temp;
	}
}
