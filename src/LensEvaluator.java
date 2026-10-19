/** One lens of CollegeFit. Each implementation judges a college from one point of view. */
public interface LensEvaluator {
    EvaluationResult evaluate(StudentProfile student, College college);
}
