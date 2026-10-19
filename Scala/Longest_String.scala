Scala Programs:
Given a string s, find the length of the longest substring without duplicate characters.
 
Example 1:
Input: s = "abcabcbb"
Output: 3
Explanation: The answer is "abc", with the length of 3. Note that "bca" and "cab" are also correct answers.
Example 2:
Input: s = "bbbbb"
Output: 1
Explanation: The answer is "b", with the length of 1.
Example 3:
Input: s = "pwwkew"
Output: 3
Explanation: The answer is "wke", with the length of 3.
Notice that the answer must be a substring, "pwke" is a subsequence and not a substring.
 
Constraints:
•	0 <= s.length <= 105
•	s consists of English letters, digits, symbols and spaces.

Solution:
object Main {
  def main(args: Array[String]): Unit = {
    println("Hello, World!")

    def maxLengthSubstring(s: String) = {
        var maxLength = 0

        for (i <- 0 to s.length) { // 0 -> 7
            var seen = Set[Char]()
            var length1 = 0
            var j = i

            while(j<s.length && !seen.contains(s(j))) { // 0<7 && 
                seen+=s(j) // seen = a
                println(s"Seen: $seen")
                length1+=1
                maxLength = Math.max(maxLength, length1)
                j+=1
            }
        }
        println(s"Hii:::$maxLength")
    }
    maxLengthSubstring("abcbcaa")
  }
}
Output: Hello, World! 
Seen: Set(a)
 Seen: Set(a, b) 
Seen: Set(a, b, c) 
Seen: Set(b) 
Seen: Set(b, c) 
Seen: Set(c)
 Seen: Set(c, b) Seen: Set(b) Seen: Set(b, c) Seen: Set(b, c, a) Seen: Set(c) Seen: Set(c, a) Seen: Set(a) Seen: Set(a) Hii:::3

Solution 2 with Sliding Window:
object Solution {
    def lengthOfLongestSubstring(s: String): Int = {

        var left = 0
        var maxLength = 0
        var seen = Set[Char]()
        for (right <- 0 until s.length) {
            while(seen.contains(s(right))) {
                seen -= s(left)
                //length +=1
                left+=1
            }
            seen += s(right)
           // length+=1
            maxLength = Math.max(maxLength, right-left+1)
        }
        maxLength
    }
    lengthOfLongestSubstring("abcbcaa")
}
