import pandas as pd
from scipy.sparse import csr_matrix
import pymysql
from CollaborativeFiltering import CollaborativeFiltering

# Step 1: Load Interaction Data from MySQL
def load_interaction_data(host, user, password, database):
    """
    Load user-article interactions from the database.
    :param host: Database host
    :param user: Database user
    :param password: Database password
    :param database: Database name
    :return: DataFrame with columns [user_id, article_id, interaction]
    """
    conn = pymysql.connect(host=host, user=user, password=password, database=database)
    query = "SELECT user_id, article_id, interaction FROM user_article_interactions"
    df = pd.read_sql_query(query, conn)
    conn.close()
    return df

# Step 2: Create User-Item Matrix
def create_user_item_matrix(df):
    user_map = {user_id: idx for idx, user_id in enumerate(df['user_id'].unique())}
    article_map = {article_id: idx for idx, article_id in enumerate(df['article_id'].unique())}
    df['user_idx'] = df['user_id'].map(user_map)
    df['article_idx'] = df['article_id'].map(article_map)
    user_item_matrix = csr_matrix((df['interaction'], (df['user_idx'], df['article_idx'])))
    return user_item_matrix, user_map, article_map

# Step 3: Main function to run everything
if __name__ == "__main__":
    # Database credentials
    host = "localhost"
    user = "root"
    password = "amna"
    database = "news_scraper"

    # Load interaction data
    interaction_data = load_interaction_data(host, user, password, database)
    print("Loaded Interaction Data:")
    print(interaction_data.head())

    # Create user-item matrix
    user_item_matrix, user_map, article_map = create_user_item_matrix(interaction_data)

    # Initialize Collaborative Filtering
    cf = CollaborativeFiltering(user_item_matrix, user_map, article_map)

    # Compute user similarity
    cf.compute_similarity()

    # Generate recommendations for a user
    user_id = 1  # Example user ID
    recommendations = cf.recommend(user_id, top_n=5)
    print(f"Recommendations for User {user_id}: {recommendations}")
