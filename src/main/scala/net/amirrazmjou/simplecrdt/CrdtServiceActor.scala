package net.amirrazmjou.simplecrdt

import akka.actor.Actor

/**
  * Created by Amir Razmjou on 2/12/17.
  */
// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class CrdtServiceActor extends Actor with CrdtService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}
