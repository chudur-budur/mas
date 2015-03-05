Notes on Inverse-RL experiments
===============================

#### 1. One Pass:
 HiTAB is one pass, once it is learned, there is no way to modify/
 improve. But in the case of IRL, reward function can be used to 
 build similar behavior from the existing ones automatically and 
 the IRL based behavior modeling does not require further expert 
 demos.

#### 2. Feature Dependency:
 Dependent on feature, if a transition is learned on feature set 
 F1, it is not transferable to another robot if it is only dependent 
 on feature sets F2. For example if a is learned to do wall-follow on 
 left sonar only, the learned transition functions can't be transferred 
 to another robot to do wall-follow on opposite direction.

#### 3. Decomposability on the Feature Space:
 If you want to build a behavior that requires {F1 U F2} feature sets, 
 then you need to train the transition functions on {F1 U F2} in HiTAB. 
 But in this IRL case, you can build a reward function R1 <- {F1} and 
 another reward function R2 <- {F2}, then the agent can learn a behaviour 
 that needs {F1 U F2} by optimizing a combinations of reward function like 
 w1*R1 + w2*R2.

#### 4. Learning Unbounded-Length Policy:
 *All* IRL depends on a policy with fixed length, expert demos are 
 considered as a set of trajectories. But in HiTAB we can model a 
 policy with unbounded length -- expert demos are interpreted as 
 FSA. 

#### 5. Limitations in Real Space:
 But there is a small subtlety, the traditional meaning of a policy 
 or a control is interpreted as sequences of State -> Action tuples,
 i.e. pi = {(S1,A1), (S2, A2) -> ... (Sn, An)} etc. Moreover, in all 
 IRL algorithms, reward functions are formulated on the traditional 
 notion of policy. They build the reward function from the feature 
 values f(.) in a particular state s, r <- g(f(s)). So, we must need to 
 have a enumerable set of states, and if states are laid on real valued
 space, thus the only way to do IRL is to discretize the space and
 enumerate each Si of them. Thus the canonical IRL's are problematic
 to formulate for the FSA.

#### 6. IRL for FSA's:
 To make IRL work with FSA's, we need to make small change the notion of
 policy. We will consider the the basic behavior entity as a state. For 
 example, if there is a transition function t(.) that maps one behavior B1
 to another behaviour B2, the transition is modeled as t(B1) --> B2. This 
 transition function is also dependent on the feature values before the 
 transition, i.e. t(B1, f(.)) --> B2. Here, f(.) means the feature values 
 at the state s, i.e. f(s) and in the case of HiTAB -- s is implied (not 
 specifiable). If we want to interpret this scenario using the canonical 
 notion of policy, then -- 
 	
 	pi = {([B1, f(.)], B2), ([B2, f(.)], B3), ([B3, f(.)], Bj)
 		... [(Bj, f(.)],Bn)... }
 
 and thus the policy length becomes unbounded and the states and actions 
 need to be interpreted as Si -> [(Bi, f(.))], Ai -> Bj.

  * Reinterpretation for IRL:
     Now in the IRL case, we will consider each source and target 
     behavior as states, Bi is the start state and Bj is the target 
     state and action is a transition so, a policy will look like this
     
     	pi = {(B1, [f(.), B2]), (B2, [f(.), B4]) ... }
     
     and moreover suppose that the feature values that caused a
     transition Bi -> Bj is fij(.), and what if we consider this 
     Aij -> [fij(.) Bj] as action ? now the policy looks like --
     
     	pi = {(B1, [f12(.), B2]), (B2, [f24(.), B4]), ... } 
     
     still the policy length is unbounded, but we got rid off the 
     requirement of enumerable set of states that can give us a way 
     to construct the reward function as matrix in an interesting 
     way --
     
     			Traget Behavior
     
     		     | B1 | B2 | ... | Bn |
     		---------------------------
     		B1   | x  | a  | ... | b  |
     		---------------------------
     		B2   | c  | d  | ... | e  |
Start Behavior 	---------------------------
     		.			.
     		.			.
     		---------------------------
     		Bn   | f  | g  | ... | h  |
     		---------------------------
     
     Now, this becomes obvious that each of the matrix entries are
     reward values, and they will be interpreted as --
     
        * If I was doing B1 and I read feature values f13(.)
     	  i.e. (B1->B3), and then I start doing B2, what will 
     	  be my reward?
     
     	* In the above case the agent will get some punishment
     	  because, the robot read feature values similar to f13(.)
     	  , which means the agent needs to do behavior B3. But the
     	  robot is doing B2, etc.
     
     Now, the next part is to calculate these reward values. Obviously
     the reward values will be calculated from the expert demonstrations.
     Although the algorithm is inspired from Nguen & Abbeel's work but
     there are some small modifications.  		
	 
#### 7. Rebuilding FSA from the Q-table:
	To learn a behaviour is now straight forward, learn the above function using 
	generic Q-learning algorithm, and obviously the Q-table structure is same as
	the reward matrix, where each of the matrix entries are the corresponding Q-values. 

	The interesting part is to rebuild the automata from the learned Q-table. The 
	algorithm is simple. After the Q-table is being learned, do the maneuver for 
	one more time and during the behavior transition record the examples. From these
	transition and recorded examples, we will be able to construct the FSA again.

#### 8. Transfer the reward R-table or the Q-table?
	We need to transfer R-table, not Q. Because Q-table may be constructed from
	a particular perspective which may not be applicable to other scenarios. 
