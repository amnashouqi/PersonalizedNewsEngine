import numpy as np

class CollaborativeFiltering:
    def __init__(self, user_item_matrix, user_map, article_map):
        """
        Initialize with user-item matrix and mappings.
        :param user_item_matrix: Sparse matrix (users x items)
        :param user_map: Mapping from user IDs to matrix indices
        :param article_map: Mapping from article IDs to matrix indices
        """
        self.user_item_matrix = user_item_matrix
        self.user_map = user_map
        self.article_map = article_map
        self.user_similarity = None

    def compute_similarity(self):
        """Compute user-user cosine similarity."""
        self.user_similarity = cosine_similarity(self.user_item_matrix)

    def recommend(self, user_id, top_n=5):
        """
        Generate article recommendations for a given user.
        :param user_id: ID of the user for whom to generate recommendations.
        :param top_n: Number of top recommendations to generate.
        :return: List of recommended article IDs.
        """
        if user_id not in self.user_map:
            print(f"User ID {user_id} not found in the dataset.")
            return []

        user_idx = self.user_map[user_id]

        # Calculate scores for all articles
        user_similarities = self.user_similarity[user_idx]
        scores = user_similarities.dot(self.user_item_matrix).flatten()

        # Zero out scores for articles the user has already interacted with
        interacted_items = self.user_item_matrix[user_idx].indices
        scores[interacted_items] = 0

        # Get top N article indices
        top_article_indices = scores.argsort()[-top_n:][::-1]

        # Map indices back to article IDs
        idx_to_article = {v: k for k, v in self.article_map.items()}
        recommendations = [idx_to_article[idx] for idx in top_article_indices]

        return recommendations


def cosine_similarity(matrix):
    """
    Compute the cosine similarity between users using NumPy.
    :param matrix: A sparse user-item interaction matrix.
    :return: Cosine similarity matrix.
    """
    # Convert the matrix to dense (NumPy array) if it is sparse
    matrix = matrix.toarray() if hasattr(matrix, 'toarray') else matrix

    # Compute dot product of the matrix with its transpose
    dot_product = np.dot(matrix, matrix.T)

    # Compute the norms (magnitude) of each row in the matrix
    norms = np.linalg.norm(matrix, axis=1)

    # Compute cosine similarity matrix
    cosine_sim = dot_product / (norms[:, None] * norms[None, :])

    return cosine_sim
