package org.openmole.plugin.task

import org.openmole.core.dsl._

package object cormas {
  lazy val cormasInputs = new {
    def +=(p: Val[_], name: String): CORMASTask ⇒ CORMASTask =
      (CORMASTask.cormasInputs add (p, name)) andThen (inputs += p)
    def +=(p: Val[_]*): CORMASTask ⇒ CORMASTask = p.map(p ⇒ +=(p, p.name))
  }

  lazy val cormasOutputs = new {
    def +=(name: String, p: Val[_]): CORMASTask ⇒ CORMASTask =
      (CORMASTask.cormasOutputs add (name, p)) andThen (outputs += p)
    def +=(p: Val[_]*): CORMASTask ⇒ CORMASTask = p.map(p ⇒ +=(p.name, p))
  }
}
