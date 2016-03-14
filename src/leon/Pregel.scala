import leon.lang._
import leon.lang.synthesis._

type VertexId = Int

class PregelVerificationSkeleton[VD, ED, Message] (
  num_vertices : Int,
  edge : (VertexId, VertexId) => Boolean) {

  def isVertex (vid : VertexId) : Boolean = {
    1 <= vid && vid <= num_vertices
  }

  // Indicate whether src and dst are adjacent
  def isAdjacent (src : VertexId, dst : VertexId) : Boolean = {
    edge(src, dst) || edge(src, dst)
  }

  // Get the attribute of vertex vid in the nth round
  def vAttr (n : Int, vid : VertexId) : VD = {
    require(isVertex(vid))
    choose((attr : VD) => true)
  }

  // Get the attribute of the given edge in the nth round
  def eAttr (n : Int, src : VertexId, dst : VertexId) : ED = {
    require(edge(src, dst))
    choose((attr : ED) => true)
  }

  // Indicate whether a message is sent from src to dst in the nth round
  def activate[Message] (n : Int, src : VertexId, dst : VertexId) : Boolean = {
    require(edge(src, dst))
    choose((active : Boolean) => true)
  }

  def message[Message] (n : Int, src : VertexId, dst : VertexId) : Message = {
    require(edge(src, dst) && activate(n, src, dst))
    choose((msg : Message) => true)
  }

  def sendMessage (n : Int, src : VertexId, dst : VertexId) : List[(VertexId, Message)] = {
    require(edge(src, dst))
    ???
  } ensuring { list : List[(Int, Message)] =>
    forall((vid : VertexId, msg : Message) =>
      (list.contains((vid, msg), msg) ==> (vid == src || vid == dst))
    ) && forall((msg : Message) =>
      (list.contains((src, msg)) ==>
        (message(n + 1, dst, src) == msg && activate(n + 1, dst, src))) &&
        (list.contains((dst, msg)) ==>
          (message(n + 1, src, dst) == msg && activate(n + 1, src, dst))) &&
        (forall((msg : Message) => !list.contains((src, msg)))) ==> !activate(n + 1, dst, src) &&
        (forall((msg : Message) => !list.contains((dst, msg)))) ==> !activate(n + 1, src, dst))
  }

  // Define a total ordering on messages.
  // Needed to assert merge functions such as min and max.
  def ordering[Message] (m1 : Message, m2 : Message) : Boolean = ???

  // Reasonable candidates of mergeMsg are: min, max, choose, and sum.
  def mergeMsg_minmax[Message] (m1 : Message, m2 : Message) : Message = {
    if (ordering(m1, m2)) m1 else m2
  } ensuring (res => ordering(res, m1) && ordering(res, m2))

  def mergeMsg_choose[Message] (m1 : Message, m2 : Message) : Message = {
    m1 // or m2
  } ensuring (res => res == m1 || res == m2)

  def mergeMsg_sum (m1 : Int, m2 : Int) : Int = {
    m1 + m2
  }

  def mergeMsg[Message] (m1 : Message, m2 : Message) : Message = {
    ???
  }

  def vprog[Message] (n : Int, vid : VertexId, state : VD, msg : Message) : VD = {
    require(exists((other : VertexId) => activate(n, other, vid)))

    // This pre-condition is for merge functions such as sum
    require(/* isAssociative(mergeMsg) && isCommutative(mergeMsg) && */
      exists((acc : VertexId => Int) =>
        msg == acc(num_vertices) &&
          forall((k : VertexId) =>
            ((k <= 0) ==> (acc(k) == 0)) &&
              ((k > 0 && k <= num_vertices) ==> (acc(k) == (
                if (!activate(n, k, vid)) acc(k - 1)
                else mergeMsg(acc(k - 1), message(n, k, vid)))))
          )))
    // This pre-condition is a special case of the above condition
    // when the merge function is min, max, etc.
    require(forall((other : VertexId, other_msg : Message) =>
      activate(n, other, vid) ==> ordering(msg, message(n, other, vid))))
    ???
  } ensuring { res =>
    vAttr(n + 1, vid) == res
  }

  val initial_message : Message = ???
  val initial_vertex_state : VD = ???

  def PregelLoop = {
    require(forall((vid : VertexId) =>
      message(0, vid, vid) == initial_message &&
        vAttr(0, vid) == initial_vertex_state))
    var n_iter = 0
    while (exists((v1 : VertexId, v2 : VertexId) => activate(n_iter, v1, v2))) {
      1 to num_vertices foreach (v1 =>
        1 to num_vertices foreach (v2 =>
          sendMessage(n_iter, v1, v2)))
      1 to num_vertices foreach (dst => {
        val messages = 1 to num_vertices filter (activate(n_iter, _, dst)) map (message(n_iter, _, dst))
        if (messages.length > 0) {
          vprog(n_iter, dst, vAttr(n_iter, dst), messages.reduce(mergeMsg))
        }
      })
      n_iter = n_iter + 1
    }
  }

  def isAssociative[A] (f : (A, A) => A) : Boolean = {
    forall((x : A, y : A, z : A) => f(f(x, y), z) == f(x, f(y, z)))
  }

  def isCommutative[A] (f : (A, A) => A) : Boolean = {
    forall((x : A, y : A) => f(x, y) == f(y, x))
  }

  def exists[A] (p : A => Boolean) = !forall[A](!p(_))
  def exists[A, B] (p : (A, B) => Boolean) = !forall[A, B]((a, b) => !p(a, b))
  def exists[A, B, C] (p : (A, B, C) => Boolean) = !forall[A, B, C]((a, b, c) => !p(a, b, c))
}

