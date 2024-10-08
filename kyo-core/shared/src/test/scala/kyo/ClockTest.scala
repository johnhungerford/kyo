package kyo

import java.time.Instant
import java.time.temporal.ChronoUnit
import kyo.Clock.Deadline
import kyo.Clock.Stopwatch

class ClockTest extends Test:

    given CanEqual[Instant, Instant] = CanEqual.derived

    class TestClock extends Clock:
        var currentTime = Instant.now()

        val unsafe: Clock.Unsafe = new Clock.Unsafe:
            def now()(using AllowUnsafe): Instant = currentTime
        def now(using Frame) = IO(currentTime)

        def advance(duration: Duration): Unit =
            currentTime = currentTime.plus(duration.toJava)

        def set(instant: Instant): Unit =
            currentTime = instant
    end TestClock

    "Clock" - {
        "now" in run {
            val testClock = new TestClock
            val instant   = testClock.currentTime
            for
                result <- Clock.let(testClock)(Clock.now)
            yield assert(result == instant)
        }

        "unsafe now" in {
            import AllowUnsafe.embrace.danger
            val testClock = new TestClock
            val instant   = testClock.currentTime
            assert(testClock.unsafe.now() == instant)
        }

        "now at epoch" in run {
            val testClock = new TestClock
            testClock.set(Instant.EPOCH)
            for
                result <- Clock.let(testClock)(Clock.now)
            yield assert(result == Instant.EPOCH)
        }

        "now at max instant" in run {
            val testClock = new TestClock
            testClock.set(Instant.MAX)
            for
                result <- Clock.let(testClock)(Clock.now)
            yield assert(result == Instant.MAX)
        }
    }

    "Stopwatch" - {
        "elapsed time" in run {
            val testClock = new TestClock
            for
                stopwatch <- Clock.let(testClock)(Clock.stopwatch)
                _ = testClock.advance(5.seconds)
                elapsed <- stopwatch.elapsed
            yield assert(elapsed == 5.seconds)
            end for
        }

        "unsafe elapsed time" in {
            import AllowUnsafe.embrace.danger
            val testClock = new TestClock
            val stopwatch = testClock.unsafe.stopwatch()
            testClock.advance(5.seconds)
            assert(stopwatch.elapsed() == 5.seconds)
        }

        "zero elapsed time" in run {
            val testClock = new TestClock
            for
                stopwatch <- Clock.let(testClock)(Clock.stopwatch)
                elapsed   <- stopwatch.elapsed
            yield assert(elapsed == Duration.Zero)
            end for
        }
    }

    "Deadline" - {
        "timeLeft" in run {
            val testClock = new TestClock
            for
                deadline <- Clock.let(testClock)(Clock.deadline(10.seconds))
                _ = testClock.advance(3.seconds)
                timeLeft <- deadline.timeLeft
            yield assert(timeLeft == 7.seconds)
            end for
        }

        "isOverdue" in run {
            val testClock = new TestClock
            for
                deadline   <- Clock.let(testClock)(Clock.deadline(5.seconds))
                notOverdue <- deadline.isOverdue
                _ = testClock.advance(6.seconds)
                overdue <- deadline.isOverdue
            yield assert(!notOverdue && overdue)
            end for
        }

        "unsafe timeLeft" in {
            import AllowUnsafe.embrace.danger
            val testClock = new TestClock
            val deadline  = testClock.unsafe.deadline(10.seconds)
            testClock.advance(3.seconds)
            assert(deadline.timeLeft() == 7.seconds)
        }

        "unsafe isOverdue" in {
            import AllowUnsafe.embrace.danger
            val testClock = new TestClock
            val deadline  = testClock.unsafe.deadline(5.seconds)
            assert(!deadline.isOverdue())
            testClock.advance(6.seconds)
            assert(deadline.isOverdue())
        }

        "zero duration deadline" in run {
            val testClock = new TestClock
            for
                deadline  <- Clock.let(testClock)(Clock.deadline(Duration.Zero))
                isOverdue <- deadline.isOverdue
                timeLeft  <- deadline.timeLeft
            yield assert(!isOverdue && timeLeft == Duration.Zero)
            end for
        }

        "deadline exactly at expiration" in run {
            val testClock = new TestClock
            for
                deadline <- Clock.let(testClock)(Clock.deadline(5.seconds))
                _ = testClock.advance(5.seconds)
                isOverdue <- deadline.isOverdue
                timeLeft  <- deadline.timeLeft
            yield assert(!isOverdue && timeLeft == Duration.Zero)
            end for
        }
    }

    "Integration" - {
        "using stopwatch with deadline" in run {
            val testClock = new TestClock
            for
                stopwatch <- Clock.let(testClock)(Clock.stopwatch)
                deadline  <- Clock.let(testClock)(Clock.deadline(10.seconds))
                _ = testClock.advance(7.seconds)
                elapsed  <- stopwatch.elapsed
                timeLeft <- deadline.timeLeft
            yield assert(elapsed == 7.seconds && timeLeft == 3.seconds)
            end for
        }

        "multiple stopwatches and deadlines" in run {
            val testClock = new TestClock
            for
                stopwatch1 <- Clock.let(testClock)(Clock.stopwatch)
                deadline1  <- Clock.let(testClock)(Clock.deadline(10.seconds))
                _ = testClock.advance(3.seconds)
                stopwatch2 <- Clock.let(testClock)(Clock.stopwatch)
                deadline2  <- Clock.let(testClock)(Clock.deadline(5.seconds))
                _ = testClock.advance(4.seconds)
                elapsed1  <- stopwatch1.elapsed
                elapsed2  <- stopwatch2.elapsed
                timeLeft1 <- deadline1.timeLeft
                timeLeft2 <- deadline2.timeLeft
            yield
                assert(elapsed1 == 7.seconds)
                assert(elapsed2 == 4.seconds)
                assert(timeLeft1 == 3.seconds)
                assert(timeLeft2 == 1.second)
            end for
        }
    }
end ClockTest
