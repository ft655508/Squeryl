/*******************************************************************************
 * Copyright 2010 Maxime Lévesque 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.squeryl

import annotations.Transient

/**
 *  For use with View[A] or Table[A], when A extends KeyedEntity[K],
 * lookup and delete by key become implicitely available
 * Example :
 *
 * class Peanut(weight: Float) extends KeyedEntity[Long]
 * val peanutJar = Table[Peanut]
 *
 * Since Peanut extends KeyedEntity the delete(l:Long)
 * method is available
 *  
 * def removePeanut(idOfThePeanut: Long) =
 *   peanutJar.delete(idOfThePeanut)
 *
 * And lookup by id is also implicitely available :
 * 
 * peanutJar.lookup(idOfThePeanut)
 *
 */

trait KeyedEntity[K] extends PersistenceStatus {

  def id: K
}


trait PersistenceStatus {
  
  @Transient
  private [squeryl] var _isPersisted = false

  def isPersisted: Boolean = _isPersisted
}


trait IndirectKeyedEntity[K,T] extends KeyedEntity[K] {
  
  def idField: T
}


trait Optimistic {
  self: KeyedEntity[_] =>

  protected val occVersionNumber = 0
}

class StaleUpdateException(message: String) extends RuntimeException(message)

trait EntityMember {

  def entityRoot[B]: Query[B]
}


trait ReferentialAction {
  def event: String
  def action: String
}

/**
 * ForeingKeyDeclaration are to be manipulated only during the Schema definition
 * (this is why all public methods have the implicit arg (implicit ev: Schema))
 */
class ForeingKeyDeclaration(val idWithinSchema: Int, val foreingKeyColumnName: String, val referencedPrimaryKey: String) {

  private var _referentialActions: Option[(Option[ReferentialAction],Option[ReferentialAction])] = None

  private [squeryl] def _isActive =
    _referentialActions != None

  private [squeryl] def _referentialAction1: Option[ReferentialAction] =
    _referentialActions.get._1

  private [squeryl] def _referentialAction2: Option[ReferentialAction] =
    _referentialActions.get._2

  /**
   * Causes the foreing key to have no constraint 
   */
  def unConstrainReference()(implicit ev: Schema) =
    _referentialActions = None

  /**
   * Will cause a foreing key constraint to be created at schema creation time :
   * alter table <tableOfForeingKey> add foreing key (<foreingKey>) references <tableOfPrimaryKey>(<primaryKey>)
   */
  def constrainReference()(implicit ev: Schema) =
    _referentialActions = Some((None, None))

  /**
   * Does the same as constrainReference, plus adds a ReferentialAction (ex.: foreingKeyDeclaration.constrainReference(onDelete cascade)) 
   */
  def constrainReference(a1: ReferentialAction)(implicit ev: Schema) =
    _referentialActions = Some((Some(a1), None))

  /**
   * Does the same as constrainReference, plus adds two ReferentialActions
   * (ex.: foreingKeyDeclaration.constrainReference(onDelete cascade, onUpdate restrict)) 
   */
  def constrainReference(a1: ReferentialAction, a2: ReferentialAction)(implicit ev: Schema) =
    _referentialActions = Some((Some(a1), Some(a2)))
}
