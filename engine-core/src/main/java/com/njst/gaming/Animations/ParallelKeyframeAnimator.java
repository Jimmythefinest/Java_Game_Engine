package com.njst.gaming.Animations;

import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ParallelKeyframeAnimator {
    private static final float LEGACY_FRAMES_PER_SECOND = 60f;
    private static final int WORKER_COUNT = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final int PARALLEL_THRESHOLD = 4;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            WORKER_COUNT,
            new ThreadFactory() {
                private final AtomicInteger threadIndex = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable,
                            "NJST-KeyframeAnimator-" + threadIndex.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private ParallelKeyframeAnimator() {
    }

    public static void animate(List<KeyframeAnimation> animations, float deltaSeconds) {
        if (animations == null || animations.isEmpty()) {
            return;
        }
        if (animations.size() < PARALLEL_THRESHOLD || WORKER_COUNT <= 1) {
            animateSequentially(animations, deltaSeconds);
            return;
        }

        ArrayList<AnimationJob> jobs = new ArrayList<>();
        for (KeyframeAnimation animation : animations) {
            AnimationJob job = createJob(animation, deltaSeconds);
            if (job != null) {
                jobs.add(job);
            }
        }
        if (jobs.isEmpty()) {
            return;
        }

        ArrayList<Future<AnimationResult>> futures = new ArrayList<>(jobs.size());
        for (AnimationJob job : jobs) {
            futures.add(EXECUTOR.submit(new Callable<AnimationResult>() {
                @Override
                public AnimationResult call() {
                    return evaluate(job);
                }
            }));
        }

        ArrayList<AnimationResult> results = new ArrayList<>(futures.size());
        try {
            for (Future<AnimationResult> future : futures) {
                results.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            animateSequentially(animations, deltaSeconds);
            return;
        } catch (ExecutionException e) {
            animateSequentially(animations, deltaSeconds);
            return;
        }

        // Only the frame thread mutates animation state, bones, and finish callbacks.
        for (AnimationResult result : results) {
            apply(result);
        }
    }

    public static void animateSkeletons(List<? extends List<KeyframeAnimation>> skeletonAnimationBatches,
                                        float deltaSeconds) {
        if (skeletonAnimationBatches == null || skeletonAnimationBatches.isEmpty()) {
            return;
        }
        if (skeletonAnimationBatches.size() < 2 || WORKER_COUNT <= 1) {
            for (List<KeyframeAnimation> skeletonAnimations : skeletonAnimationBatches) {
                animateSequentially(skeletonAnimations, deltaSeconds);
            }
            return;
        }

        ArrayList<Future<ArrayList<AnimationResult>>> futures = new ArrayList<>();
        for (List<KeyframeAnimation> skeletonAnimations : skeletonAnimationBatches) {
            ArrayList<AnimationJob> jobs = createJobs(skeletonAnimations, deltaSeconds);
            if (jobs.isEmpty()) {
                continue;
            }
            futures.add(EXECUTOR.submit(new Callable<ArrayList<AnimationResult>>() {
                @Override
                public ArrayList<AnimationResult> call() {
                    ArrayList<AnimationResult> results = new ArrayList<>(jobs.size());
                    for (AnimationJob job : jobs) {
                        results.add(evaluate(job));
                    }
                    return results;
                }
            }));
        }

        ArrayList<ArrayList<AnimationResult>> skeletonResults = new ArrayList<>(futures.size());
        try {
            for (Future<ArrayList<AnimationResult>> future : futures) {
                skeletonResults.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            animateSkeletonsSequentially(skeletonAnimationBatches, deltaSeconds);
            return;
        } catch (ExecutionException e) {
            animateSkeletonsSequentially(skeletonAnimationBatches, deltaSeconds);
            return;
        }

        // Apply all skeleton results on the frame thread after every worker has joined.
        for (ArrayList<AnimationResult> results : skeletonResults) {
            for (AnimationResult result : results) {
                apply(result);
            }
        }
    }

    private static void animateSequentially(List<KeyframeAnimation> animations, float deltaSeconds) {
        for (KeyframeAnimation animation : animations) {
            if (animation != null) {
                animation.animate(deltaSeconds);
            }
        }
    }

    private static void animateSkeletonsSequentially(List<? extends List<KeyframeAnimation>> skeletonAnimationBatches,
                                                     float deltaSeconds) {
        for (List<KeyframeAnimation> skeletonAnimations : skeletonAnimationBatches) {
            animateSequentially(skeletonAnimations, deltaSeconds);
        }
    }

    private static ArrayList<AnimationJob> createJobs(List<KeyframeAnimation> animations, float deltaSeconds) {
        ArrayList<AnimationJob> jobs = new ArrayList<>();
        if (animations == null) {
            return jobs;
        }
        for (KeyframeAnimation animation : animations) {
            AnimationJob job = createJob(animation, deltaSeconds);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private static AnimationJob createJob(KeyframeAnimation animation, float deltaSeconds) {
        if (animation == null || !animation.active || animation.bone == null
                || animation.keyframes == null || animation.keyframes.isEmpty()) {
            return null;
        }
        float safeDeltaSeconds = Math.max(0f, deltaSeconds);
        float framesPerSecond = animation.framesPerSecond > 0f
                ? animation.framesPerSecond
                : LEGACY_FRAMES_PER_SECOND;
        float nextTime = animation.time + safeDeltaSeconds * framesPerSecond * animation.speed;
        KeyframeAnimation.Keyframe previousKeyframe = null;
        KeyframeAnimation.Keyframe nextKeyframe = null;

        for (KeyframeAnimation.Keyframe keyframe : animation.keyframes) {
            if (keyframe.time <= nextTime) {
                previousKeyframe = keyframe;
            } else if (nextKeyframe == null) {
                nextKeyframe = keyframe;
                break;
            }
        }

        if (nextKeyframe == null) {
            return new AnimationJob(animation, nextTime, null, null, 0f, true, false);
        }
        if (previousKeyframe == null) {
            return new AnimationJob(animation, nextTime, null, null, 0f, false, false);
        }

        float frameSpan = nextKeyframe.time - previousKeyframe.time;
        float blend = frameSpan != 0f ? (nextTime - previousKeyframe.time) / frameSpan : 1f;
        return new AnimationJob(
                animation,
                nextTime,
                previousKeyframe.rotation.clone(),
                nextKeyframe.rotation.clone(),
                blend,
                false,
                true);
    }

    private static AnimationResult evaluate(AnimationJob job) {
        if (!job.hasRotation || job.finished) {
            return new AnimationResult(job, null);
        }
        Quaternion start = Quaternion.fromEuler(job.startRotation.x, job.startRotation.y, job.startRotation.z);
        Quaternion end = Quaternion.fromEuler(job.endRotation.x, job.endRotation.y, job.endRotation.z);
        Quaternion interpolated = Quaternion.slerp(start, end, job.blend);
        return new AnimationResult(job, new Vector3(interpolated.toEuler()));
    }

    private static void apply(AnimationResult result) {
        KeyframeAnimation animation = result.job.animation;
        animation.time = result.job.time;
        if (result.job.finished) {
            if (animation.onfinish != null) {
                animation.onfinish.run();
            }
            return;
        }
        if (result.rotation != null && animation.bone != null) {
            animation.bone.setRotation(result.rotation);
        }
    }

    private static final class AnimationJob {
        final KeyframeAnimation animation;
        final float time;
        final Vector3 startRotation;
        final Vector3 endRotation;
        final float blend;
        final boolean finished;
        final boolean hasRotation;

        AnimationJob(KeyframeAnimation animation,
                     float time,
                     Vector3 startRotation,
                     Vector3 endRotation,
                     float blend,
                     boolean finished,
                     boolean hasRotation) {
            this.animation = animation;
            this.time = time;
            this.startRotation = startRotation;
            this.endRotation = endRotation;
            this.blend = blend;
            this.finished = finished;
            this.hasRotation = hasRotation;
        }
    }

    private static final class AnimationResult {
        final AnimationJob job;
        final Vector3 rotation;

        AnimationResult(AnimationJob job, Vector3 rotation) {
            this.job = job;
            this.rotation = rotation;
        }
    }
}
